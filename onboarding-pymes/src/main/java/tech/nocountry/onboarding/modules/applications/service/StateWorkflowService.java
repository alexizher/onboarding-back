package tech.nocountry.onboarding.modules.applications.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.nocountry.onboarding.entities.ApplicationStatusHistory;
import tech.nocountry.onboarding.entities.CreditApplication;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.enums.ApplicationStatus;
import tech.nocountry.onboarding.modules.applications.dto.StatusHistoryResponse;
import tech.nocountry.onboarding.modules.applications.events.ApplicationStatusChangedEvent;
import tech.nocountry.onboarding.repositories.ApplicationStatusHistoryRepository;
import tech.nocountry.onboarding.repositories.CreditApplicationRepository;
import tech.nocountry.onboarding.repositories.UserRepository;
import tech.nocountry.onboarding.repositories.DocumentRepository;
import tech.nocountry.onboarding.repositories.DocumentTypeRepository;

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
    private final DocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final tech.nocountry.onboarding.services.AuditLogService auditLogService;

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
        VALID_TRANSITIONS.put(ApplicationStatus.PENDING.name(), List.of(
            ApplicationStatus.SUBMITTED.name(),
            ApplicationStatus.CANCELLED.name()
        ));
    }

    @Transactional
    public CreditApplication changeStatus(String applicationId, String newStatus, String userId, String comments) {
        log.info("Changing status for application: {} to: {} by user: {}", applicationId, newStatus, userId);

        // Obtener la aplicación con el user cargado (fetch join para evitar lazy loading)
        CreditApplication application = applicationRepository.findByApplicationIdWithUser(applicationId)
                .orElseGet(() -> {
                    // Fallback a método normal si no se encuentra con fetch join
                    log.warn("No se encontró aplicación con fetch join, intentando método normal");
                    return applicationRepository.findByApplicationId(applicationId)
                            .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
                });

        // Obtener el usuario
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Normalizar el rol quitando el prefijo "ROLE_" si existe
        String userRole = user.getRole() != null ? user.getRole().getRoleId() : null;
        if (userRole != null && userRole.startsWith("ROLE_")) {
            userRole = userRole.substring(5); // Quitar "ROLE_"
        }
        String previousStatus = application.getStatus();

        // Validaciones
        if (!isValidTransition(previousStatus, newStatus)) {
            throw new RuntimeException(String.format(
                "Transición inválida de '%s' a '%s'. Transiciones permitidas: %s",
                previousStatus, newStatus, VALID_TRANSITIONS.getOrDefault(previousStatus, List.of())
            ));
        }
        if (!isAllowedForRole(userRole, newStatus)) {
            throw new RuntimeException(String.format(
                "El rol '%s' no tiene permiso para cambiar al estado '%s'",
                userRole, newStatus
            ));
        }
        validateRolePermission(application, userRole, previousStatus, newStatus);

        // Actualizar el estado
        application.setStatus(newStatus);
        applicationRepository.save(application);
        
        // Recargar la aplicación con el user cargado (fetch join) para obtener el userId del dueño
        CreditApplication updatedWithUser = applicationRepository.findByApplicationIdWithUser(applicationId)
                .orElseGet(() -> {
                    log.warn("No se encontró aplicación con fetch join, intentando método normal");
                    return applicationRepository.findByApplicationId(applicationId)
                            .orElseThrow(() -> new RuntimeException("Solicitud no encontrada después de actualizar"));
                });

        // Registrar en el historial
        recordStatusChange(applicationId, previousStatus, newStatus, userRole, user, comments);

        // Obtener userId del dueño de la aplicación
        String applicationOwnerUserId;
        try {
            if (updatedWithUser.getUser() == null) {
                log.error("User es null en aplicación {} después de recargar con fetch join", applicationId);
                throw new RuntimeException("User no disponible en aplicación");
            }
            
            applicationOwnerUserId = updatedWithUser.getUser().getUserId();
            
            if (applicationOwnerUserId == null || applicationOwnerUserId.isBlank()) {
                log.error("UserId del dueño de la aplicación es null o vacío para aplicación {}", applicationId);
                throw new RuntimeException("UserId del dueño de la aplicación no disponible");
            }
            
            log.info("Publicando evento de cambio de estado - aplicación: {}, dueño: {}, {} -> {}", 
                     applicationId, applicationOwnerUserId, previousStatus, newStatus);
        } catch (Exception e) {
            log.error("Error al obtener userId del dueño de la aplicación {}: {}", applicationId, e.getMessage(), e);
            throw new RuntimeException("Error al obtener información del dueño de la aplicación", e);
        }

        // Publicar evento
        applicationEventPublisher.publishEvent(new ApplicationStatusChangedEvent(
                this, applicationId, previousStatus, newStatus, applicationOwnerUserId
        ));

        // Auditoría
        auditLogService.record(userId, "APPLICATION_STATUS_CHANGE", null,
                "Solicitud " + applicationId + ": " + previousStatus + " -> " + newStatus);

        log.info("Status changed successfully from {} to {}", previousStatus, newStatus);
        return updatedWithUser;
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
            validateRequiredDocuments(application.getApplicationId());
        }
    }

    private void validateRequiredDocuments(String applicationId) {
        // Obtener tipos de documentos requeridos
        var requiredTypes = documentTypeRepository.findAll().stream()
                .filter(dt -> Boolean.TRUE.equals(dt.getIsRequired()))
                .map(dt -> dt.getDocumentTypeId())
                .collect(Collectors.toSet());

        if (requiredTypes.isEmpty()) {
            return; // No hay requeridos configurados
        }

        var documents = documentRepository.findByApplicationId(applicationId);
        var providedTypes = documents.stream()
                .map(d -> d.getDocumentType().getDocumentTypeId())
                .collect(Collectors.toSet());

        requiredTypes.removeAll(providedTypes);
        if (!requiredTypes.isEmpty()) {
            throw new RuntimeException("No se puede aprobar: faltan documentos requeridos");
        }
    }

    private void recordStatusChange(String applicationId, String previousStatus, String newStatus, 
                                   String changedByRole, User changedBy, String comments) {
        CreditApplication application = applicationRepository.findByApplicationId(applicationId)
            .orElseThrow(() -> new RuntimeException("Solicitud no encontrada para historial"));
        ApplicationStatusHistory history = ApplicationStatusHistory.builder()
                .application(application)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .changedByRole(changedByRole)
                .changedBy(changedBy)
                .comments(comments)
                .changedAt(LocalDateTime.now())
                .build();

                try {
                    statusHistoryRepository.save(history);
                } catch (Exception e) {
                    log.error("ERROR al guardar historial de estado", e);
                }
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
        
        // Contar solicitudes por estado actual
        Map<String, Long> statusCounts = new HashMap<>();
        ApplicationStatus[] statuses = ApplicationStatus.values();
        for (ApplicationStatus status : statuses) {
            String statusName = status.name();
            long count = applicationRepository.countByStatus(statusName);
            statusCounts.put(statusName, count);
        }
        stats.put("byStatus", statusCounts);
        
        // Estadísticas generales
        long totalApplications = applicationRepository.count();
        long assignedApplications = applicationRepository.findAll()
                .stream()
                .filter(app -> app.getAssignedTo() != null)
                .count();
        long unassignedApplications = totalApplications - assignedApplications;
        
        stats.put("total", totalApplications);
        stats.put("assigned", assignedApplications);
        stats.put("unassigned", unassignedApplications);
        
        // Solicitudes por analista
        Map<String, Long> byAnalyst = applicationRepository.findAll()
                .stream()
                .filter(app -> app.getAssignedTo() != null)
                .collect(Collectors.groupingBy(
                    app -> {
                        try {
                            return app.getAssignedTo().getUserId();
                        } catch (Exception e) {
                            return "unknown";
                        }
                    },
                    Collectors.counting()
                ));
        stats.put("byAnalyst", byAnalyst);
        
        // Solicitudes creadas en el último mes y hoy
        java.time.LocalDateTime oneMonthAgo = java.time.LocalDateTime.now().minusMonths(1);
        java.time.LocalDateTime today = java.time.LocalDateTime.now().toLocalDate().atStartOfDay();
        java.time.LocalDateTime tomorrow = today.plusDays(1);
        
        long createdLastMonth = applicationRepository.findAll()
                .stream()
                .filter(app -> app.getCreatedAt() != null && app.getCreatedAt().isAfter(oneMonthAgo))
                .count();
        
        long createdToday = applicationRepository.findAll()
                .stream()
                .filter(app -> app.getCreatedAt() != null && 
                              app.getCreatedAt().isAfter(today) && 
                              app.getCreatedAt().isBefore(tomorrow))
                .count();
        
        stats.put("createdLastMonth", createdLastMonth);
        stats.put("createdToday", createdToday);
        
        // Monto total solicitado
        java.math.BigDecimal totalAmount = applicationRepository.findAll()
                .stream()
                .map(app -> app.getAmountRequested() != null ? app.getAmountRequested() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        stats.put("totalAmountRequested", totalAmount);
        
        // Promedio de monto solicitado
        if (totalApplications > 0) {
            java.math.BigDecimal avgAmount = totalAmount.divide(
                java.math.BigDecimal.valueOf(totalApplications), 
                2, 
                java.math.RoundingMode.HALF_UP
            );
            stats.put("averageAmountRequested", avgAmount);
        } else {
            stats.put("averageAmountRequested", java.math.BigDecimal.ZERO);
        }
        
        log.info("Statistics generated: total={}, assigned={}, unassigned={}", 
                 totalApplications, assignedApplications, unassignedApplications);
        
        return stats;
    }
}

