package tech.nocountry.onboarding.modules.applications.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.nocountry.onboarding.entities.ApplicationStatusHistory;
import tech.nocountry.onboarding.entities.CreditApplication;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.enums.ApplicationStatus;
import tech.nocountry.onboarding.modules.applications.dto.StatusChangeRequest;
import tech.nocountry.onboarding.modules.applications.dto.StatusHistoryResponse;
import tech.nocountry.onboarding.repositories.ApplicationStatusHistoryRepository;
import tech.nocountry.onboarding.repositories.CreditApplicationRepository;
import tech.nocountry.onboarding.repositories.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StateWorkflowService {

    private final CreditApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository statusHistoryRepository;
    private final UserRepository userRepository;

    // Mapa de transiciones permitidas por rol
    private static final Map<String, List<String>> ALLOWED_TRANSITIONS_BY_ROLE = new HashMap<>();
    
    static {
        // ADMIN y MANAGER pueden hacer cualquier transición
        List<String> adminTransitions = List.of(
            ApplicationStatus.DRAFT.name(),
            ApplicationStatus.SUBMITTED.name(),
            ApplicationStatus.UNDER_REVIEW.name(),
            ApplicationStatus.DOCUMENTS_PENDING.name(),
            ApplicationStatus.APPROVED.name(),
            ApplicationStatus.REJECTED.name(),
            ApplicationStatus.CANCELLED.name()
        );
        ALLOWED_TRANSITIONS_BY_ROLE.put("ADMIN", adminTransitions);
        ALLOWED_TRANSITIONS_BY_ROLE.put("MANAGER", adminTransitions);
        
        // ANALYST puede revisar y mover a under_review, documentos pendientes, aprobar o rechazar
        ALLOWED_TRANSITIONS_BY_ROLE.put("ANALYST", List.of(
            ApplicationStatus.UNDER_REVIEW.name(),
            ApplicationStatus.DOCUMENTS_PENDING.name(),
            ApplicationStatus.APPROVED.name(),
            ApplicationStatus.REJECTED.name()
        ));
        
        // APPLICANT solo puede cancelar o enviar su solicitud
        ALLOWED_TRANSITIONS_BY_ROLE.put("APPLICANT", List.of(
            ApplicationStatus.SUBMITTED.name(),
            ApplicationStatus.CANCELLED.name()
        ));
    }

    // Transiciones válidas desde cada estado
    private static final Map<String, List<String>> VALID_TRANSITIONS = new HashMap<>();
    
    static {
        VALID_TRANSITIONS.put(ApplicationStatus.DRAFT.name(), 
            List.of(ApplicationStatus.SUBMITTED.name(), ApplicationStatus.CANCELLED.name()));
        
        VALID_TRANSITIONS.put(ApplicationStatus.SUBMITTED.name(), 
            List.of(ApplicationStatus.UNDER_REVIEW.name(), ApplicationStatus.DOCUMENTS_PENDING.name(), ApplicationStatus.CANCELLED.name()));
        
        VALID_TRANSITIONS.put(ApplicationStatus.UNDER_REVIEW.name(), 
            List.of(ApplicationStatus.DOCUMENTS_PENDING.name(), ApplicationStatus.APPROVED.name(), ApplicationStatus.REJECTED.name()));
        
        VALID_TRANSITIONS.put(ApplicationStatus.DOCUMENTS_PENDING.name(), 
            List.of(ApplicationStatus.UNDER_REVIEW.name(), ApplicationStatus.REJECTED.name()));
        
        VALID_TRANSITIONS.put(ApplicationStatus.APPROVED.name(), 
            List.of(ApplicationStatus.CANCELLED.name()));
        
        VALID_TRANSITIONS.put(ApplicationStatus.REJECTED.name(), 
            List.of());
        
        VALID_TRANSITIONS.put(ApplicationStatus.CANCELLED.name(), 
            List.of());
    }

    @Transactional
    public CreditApplication changeStatus(String applicationId, String newStatus, String userId, String comments) {
        log.info("Changing status for application: {} to: {} by user: {}", applicationId, newStatus, userId);

        // Obtener la aplicación
        CreditApplication application = applicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        // Obtener el usuario
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String userRole = user.getRole() != null ? user.getRole().getName() : null;
        String previousStatus = application.getStatus();

        // Validar que el nuevo estado sea válido según el estado actual
        if (!isValidTransition(previousStatus, newStatus)) {
            throw new RuntimeException(String.format(
                "Transición inválida de '%s' a '%s'. Transiciones permitidas: %s",
                previousStatus, newStatus, VALID_TRANSITIONS.getOrDefault(previousStatus, List.of())
            ));
        }

        // Validar que el usuario tenga permiso para hacer esta transición
        if (!isAllowedForRole(userRole, newStatus)) {
            throw new RuntimeException(String.format(
                "El rol '%s' no tiene permiso para cambiar al estado '%s'",
                userRole, newStatus
            ));
        }

        // Validar permisos específicos por estado
        validateRolePermission(application, userRole, previousStatus, newStatus);

        // Actualizar el estado
        application.setStatus(newStatus);
        CreditApplication updated = applicationRepository.save(application);

        // Registrar en el historial
        recordStatusChange(applicationId, previousStatus, newStatus, userRole, user, comments);

        log.info("Status changed successfully from {} to {}", previousStatus, newStatus);

        return updated;
    }

    private boolean isValidTransition(String fromStatus, String toStatus) {
        List<String> allowedTransitions = VALID_TRANSITIONS.get(fromStatus);
        return allowedTransitions != null && allowedTransitions.contains(toStatus);
    }

    private boolean isAllowedForRole(String role, String status) {
        if (role == null) return false;
        List<String> allowedStatuses = ALLOWED_TRANSITIONS_BY_ROLE.get(role);
        return allowedStatuses != null && allowedStatuses.contains(status);
    }

    private void validateRolePermission(CreditApplication application, String role, String fromStatus, String toStatus) {
        // Un APPLICANT solo puede modificar sus propias solicitudes
        if ("APPLICANT".equals(role)) {
            if (!toStatus.equals(ApplicationStatus.CANCELLED.name()) && 
                !toStatus.equals(ApplicationStatus.SUBMITTED.name())) {
                throw new RuntimeException("Los solicitantes solo pueden cancelar o enviar sus solicitudes");
            }
        }

        // Analistas solo pueden aprobar/rechazar si tienen la solicitud asignada
        if ("ANALYST".equals(role)) {
            if ((toStatus.equals(ApplicationStatus.APPROVED.name()) || 
                 toStatus.equals(ApplicationStatus.REJECTED.name())) &&
                application.getAssignedTo() == null) {
                throw new RuntimeException("La solicitud debe estar asignada a un analista antes de aprobar o rechazar");
            }
        }

        // No se puede aprobar si faltan documentos requeridos
        if (toStatus.equals(ApplicationStatus.APPROVED.name())) {
            // TODO: Verificar que tenga todos los documentos requeridos
            log.warn("Aprobando sin validar documentos requeridos");
        }
    }

    private void recordStatusChange(String applicationId, String previousStatus, String newStatus, 
                                   String changedByRole, User changedBy, String comments) {
        ApplicationStatusHistory history = ApplicationStatusHistory.builder()
                .application(CreditApplication.builder().applicationId(applicationId).build())
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .changedByRole(changedByRole)
                .changedBy(changedBy)
                .comments(comments)
                .changedAt(LocalDateTime.now())
                .build();

        statusHistoryRepository.save(history);
        log.info("Status history recorded for application: {}", applicationId);
    }

    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> getStatusHistory(String applicationId) {
        log.info("Getting status history for application: {}", applicationId);

        List<ApplicationStatusHistory> historyList = statusHistoryRepository.findByApplicationId(applicationId);
        
        return historyList.stream()
                .map(this::mapToHistoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StatusHistoryResponse getLatestStatusChange(String applicationId) {
        log.info("Getting latest status change for application: {}", applicationId);

        ApplicationStatusHistory latest = statusHistoryRepository.findLatestByApplicationId(applicationId)
                .orElse(null);

        return latest != null ? mapToHistoryResponse(latest) : null;
    }

    private StatusHistoryResponse mapToHistoryResponse(ApplicationStatusHistory history) {
        return StatusHistoryResponse.builder()
                .historyId(history.getHistoryId())
                .applicationId(history.getApplication().getApplicationId())
                .previousStatus(history.getPreviousStatus())
                .newStatus(history.getNewStatus())
                .comments(history.getComments())
                .changedByRole(history.getChangedByRole())
                .changedBy(history.getChangedBy() != null ? history.getChangedBy().getUserId() : null)
                .changedAt(history.getChangedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<String> getAllowedTransitions(String currentStatus, String role) {
        List<String> validTransitions = VALID_TRANSITIONS.get(currentStatus);
        List<String> roleAllowedStatuses = ALLOWED_TRANSITIONS_BY_ROLE.get(role);
        
        if (validTransitions == null || roleAllowedStatuses == null) {
            return new ArrayList<>();
        }

        // Intersect: solo los estados que son válidos según el workflow Y permitidos para el rol
        return validTransitions.stream()
                .filter(roleAllowedStatuses::contains)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStatusStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        ApplicationStatus[] statuses = ApplicationStatus.values();
        for (ApplicationStatus status : statuses) {
            String statusName = status.name();
            long count = statusHistoryRepository.countByStatus(statusName);
            stats.put(statusName, count);
        }
        
        return stats;
    }
}

