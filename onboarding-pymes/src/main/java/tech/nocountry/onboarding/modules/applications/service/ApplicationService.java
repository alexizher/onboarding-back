package tech.nocountry.onboarding.modules.applications.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.nocountry.onboarding.entities.*;
import tech.nocountry.onboarding.modules.applications.dto.ApplicationRequest;
import tech.nocountry.onboarding.modules.applications.dto.ApplicationResponse;
import tech.nocountry.onboarding.modules.applications.dto.ApplicationUpdateRequest;
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

    private ApplicationResponse mapToResponse(CreditApplication application) {
        return ApplicationResponse.builder()
                .applicationId(application.getApplicationId())
                .userId(application.getUser().getUserId())
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
                .assignedTo(application.getAssignedTo() != null ? application.getAssignedTo().getUserId() : null)
                .acceptTerms(application.getAcceptTerms())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .build();
    }
}

