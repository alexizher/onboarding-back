package tech.nocountry.onboarding.modules.applications.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.nocountry.onboarding.entities.*;
import tech.nocountry.onboarding.modules.applications.dto.*;
import tech.nocountry.onboarding.modules.risk.service.RiskAssessmentService;
import tech.nocountry.onboarding.repositories.*;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApplicationService {

    private final CreditApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final BusinessCategoryRepository categoryRepository;
    private final ProfessionRepository professionRepository;
    private final CreditDestinationRepository destinationRepository;
    private final ApplicationStateRepository stateRepository;
    private final CityRepository cityRepository;
    private final RiskAssessmentService riskAssessmentService;

    @Transactional
    public ApplicationResponse createApplication(String userId, ApplicationRequest request) {
        log.info("Creating application for user: {}", userId);

        // Validar que el usuario existe
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Crear la solicitud
        CreditApplication application = CreditApplication.builder()
                .user(user)
                .companyName(request.getCompanyName())
                .cuit(request.getCuit())
                .companyAddress(request.getCompanyAddress())
                .amountRequested(request.getAmountRequested())
                .purpose(request.getPurpose())
                .creditMonths(request.getCreditMonths())
                .monthlyIncome(request.getMonthlyIncome())
                .monthlyExpenses(request.getMonthlyExpenses())
                .existingDebt(request.getExistingDebt())
                .acceptTerms(request.getAcceptTerms())
                .status("PENDING")
                .build();

        // Cargar entidades relacionadas si se proporcionaron IDs
        if (request.getCategoryId() != null) {
            categoryRepository.findById(request.getCategoryId())
                    .ifPresent(application::setCategory);
        }

        if (request.getProfessionId() != null) {
            professionRepository.findById(request.getProfessionId())
                    .ifPresent(application::setProfession);
        }

        if (request.getDestinationId() != null) {
            destinationRepository.findById(request.getDestinationId())
                    .ifPresent(application::setDestination);
        }

        if (request.getStateId() != null) {
            stateRepository.findById(request.getStateId())
                    .ifPresent(application::setState);
        }

        if (request.getCityId() != null) {
            cityRepository.findById(request.getCityId())
                    .ifPresent(application::setCity);
        }

        // Guardar la solicitud
        CreditApplication saved = applicationRepository.save(application);
        log.info("Application created with ID: {}", saved.getApplicationId());

        // Calcular evaluación de riesgo automáticamente
        try {
            riskAssessmentService.assessRiskAutomatically(saved.getApplicationId());
            log.info("Risk assessment calculated automatically for application: {}", saved.getApplicationId());
        } catch (Exception e) {
            log.warn("Error calculating risk assessment automatically for application {}: {}", 
                     saved.getApplicationId(), e.getMessage());
            // No fallar la creación si el cálculo de riesgo falla
        }

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(String applicationId) {
        log.info("Getting application by ID: {}", applicationId);
        
        CreditApplication application = applicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        return mapToResponse(application);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getUserApplications(String userId) {
        log.info("Getting applications for user: {}", userId);
        
        List<CreditApplication> applications = applicationRepository.findByUserId(userId);
        return applications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getAllApplicationsByStatus(String status) {
        log.info("Getting applications by status: {}", status);
        
        List<CreditApplication> applications = applicationRepository.findByStatus(status);
        return applications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ApplicationResponse updateApplication(String applicationId, ApplicationUpdateRequest request) {
        log.info("Updating application: {}", applicationId);
        
        CreditApplication application = applicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        // Actualizar campos si se proporcionaron
        if (request.getCompanyName() != null) {
            application.setCompanyName(request.getCompanyName());
        }
        if (request.getCuit() != null) {
            application.setCuit(request.getCuit());
        }
        if (request.getCompanyAddress() != null) {
            application.setCompanyAddress(request.getCompanyAddress());
        }
        if (request.getAmountRequested() != null) {
            application.setAmountRequested(request.getAmountRequested());
        }
        if (request.getPurpose() != null) {
            application.setPurpose(request.getPurpose());
        }
        if (request.getCreditMonths() != null) {
            application.setCreditMonths(request.getCreditMonths());
        }
        if (request.getMonthlyIncome() != null) {
            application.setMonthlyIncome(request.getMonthlyIncome());
        }
        if (request.getMonthlyExpenses() != null) {
            application.setMonthlyExpenses(request.getMonthlyExpenses());
        }
        if (request.getExistingDebt() != null) {
            application.setExistingDebt(request.getExistingDebt());
        }
        if (request.getStatus() != null) {
            application.setStatus(request.getStatus());
        }

        // Actualizar relaciones
        if (request.getCategoryId() != null) {
            categoryRepository.findById(request.getCategoryId())
                    .ifPresent(application::setCategory);
        }
        if (request.getProfessionId() != null) {
            professionRepository.findById(request.getProfessionId())
                    .ifPresent(application::setProfession);
        }
        if (request.getDestinationId() != null) {
            destinationRepository.findById(request.getDestinationId())
                    .ifPresent(application::setDestination);
        }
        if (request.getStateId() != null) {
            stateRepository.findById(request.getStateId())
                    .ifPresent(application::setState);
        }
        if (request.getCityId() != null) {
            cityRepository.findById(request.getCityId())
                    .ifPresent(application::setCity);
        }
        if (request.getAssignedTo() != null) {
            userRepository.findByUserId(request.getAssignedTo())
                    .ifPresent(application::setAssignedTo);
        }

        CreditApplication updated = applicationRepository.save(application);
        log.info("Application updated: {}", updated.getApplicationId());

        // Recalcular evaluación de riesgo automáticamente si cambió información relevante
        boolean shouldRecalculate = request.getAmountRequested() != null || 
                                    request.getMonthlyIncome() != null ||
                                    request.getMonthlyExpenses() != null ||
                                    request.getExistingDebt() != null ||
                                    request.getCategoryId() != null;
        
        if (shouldRecalculate) {
            try {
                riskAssessmentService.assessRiskAutomatically(updated.getApplicationId());
                log.info("Risk assessment recalculated automatically for application: {}", updated.getApplicationId());
            } catch (Exception e) {
                log.warn("Error recalculating risk assessment automatically for application {}: {}", 
                         updated.getApplicationId(), e.getMessage());
                // No fallar la actualización si el cálculo de riesgo falla
            }
        }

        return mapToResponse(updated);
    }

    @Transactional
    public void deleteApplication(String applicationId) {
        log.info("Deleting application: {}", applicationId);
        
        CreditApplication application = applicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        applicationRepository.delete(application);
        log.info("Application deleted: {}", applicationId);
    }

    @Transactional(readOnly = true)
    public PagedApplicationResponse filterApplications(ApplicationFilterRequest filter) {
        log.info("Filtering applications with filters: {}", filter);
        
        // Crear Pageable con ordenamiento
        Sort sort = Sort.by(
            "DESC".equalsIgnoreCase(filter.getSortDirection()) 
                ? Sort.Direction.DESC 
                : Sort.Direction.ASC,
            filter.getSortBy()
        );
        
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);
        
        // Llamar al método del repositorio con filtros
        Page<CreditApplication> page = applicationRepository.findWithFilters(
            filter.getStatus(),
            filter.getUserId(),
            filter.getAssignedToUserId(),
            filter.getCompanyName(),
            filter.getCuit(),
            filter.getCreatedFrom(),
            filter.getCreatedTo(),
            filter.getMinAmount(),
            filter.getMaxAmount(),
            pageable
        );
        
        // Convertir a PagedApplicationResponse
        List<ApplicationResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        return PagedApplicationResponse.builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    @Transactional
    public ApplicationResponse assignApplication(String applicationId, String assignedToUserId, String comments) {
        log.info("Assigning application {} to user {}", applicationId, assignedToUserId);
        
        CreditApplication application = applicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
        
        if (assignedToUserId != null && !assignedToUserId.isBlank()) {
            User assignedUser = userRepository.findByUserId(assignedToUserId)
                    .orElseThrow(() -> new RuntimeException("Usuario asignado no encontrado"));
            
            // Validar que el usuario sea un analista
            if (assignedUser.getRole() == null || 
                (!assignedUser.getRole().getRoleId().equals("ROLE_ANALYST") && 
                 !assignedUser.getRole().getRoleId().equals("ROLE_MANAGER") && 
                 !assignedUser.getRole().getRoleId().equals("ROLE_ADMIN"))) {
                throw new RuntimeException("El usuario asignado debe ser un analista, manager o admin");
            }
            
            application.setAssignedTo(assignedUser);
        } else {
            // Si assignedToUserId es null, quitar la asignación
            application.setAssignedTo(null);
        }
        
        CreditApplication updated = applicationRepository.save(application);
        log.info("Application {} assigned to user {}", applicationId, assignedToUserId);
        
        return mapToResponse(updated);
    }

    private ApplicationResponse mapToResponse(CreditApplication application) {
        // Manejar lazy loading del user
        String userId = null;
        try {
            if (application.getUser() != null) {
                userId = application.getUser().getUserId();
            }
        } catch (Exception e) {
            log.warn("Error al acceder al user de la aplicación {}: {}", application.getApplicationId(), e.getMessage());
        }
        
        // Manejar lazy loading del assignedTo
        String assignedToUserId = null;
        try {
            if (application.getAssignedTo() != null) {
                assignedToUserId = application.getAssignedTo().getUserId();
            }
        } catch (Exception e) {
            log.warn("Error al acceder al assignedTo de la aplicación {}: {}", application.getApplicationId(), e.getMessage());
        }
        
        return ApplicationResponse.builder()
                .applicationId(application.getApplicationId())
                .userId(userId)
                .status(application.getStatus())
                .companyName(application.getCompanyName())
                .cuit(application.getCuit())
                .companyAddress(application.getCompanyAddress())
                .amountRequested(application.getAmountRequested())
                .purpose(application.getPurpose())
                .creditMonths(application.getCreditMonths())
                .monthlyIncome(application.getMonthlyIncome())
                .monthlyExpenses(application.getMonthlyExpenses())
                .existingDebt(application.getExistingDebt())
                .categoryId(application.getCategory() != null ? application.getCategory().getCategoryId() : null)
                .professionId(application.getProfession() != null ? application.getProfession().getProfessionId() : null)
                .destinationId(application.getDestination() != null ? application.getDestination().getDestinationId() : null)
                .stateId(application.getState() != null ? application.getState().getStateId() : null)
                .cityId(application.getCity() != null ? application.getCity().getCityId() : null)
                .assignedTo(assignedToUserId)
                .acceptTerms(application.getAcceptTerms())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .build();
    }
}

