package tech.nocountry.onboarding.modules.applications.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import tech.nocountry.onboarding.entities.CreditApplication;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.repositories.ApplicationStatusHistoryRepository;
import tech.nocountry.onboarding.repositories.CreditApplicationRepository;
import tech.nocountry.onboarding.repositories.UserRepository;
import tech.nocountry.onboarding.services.AuditLogService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StateWorkflowServiceTest {

    @Mock
    private CreditApplicationRepository applicationRepository;

    @Mock
    private ApplicationStatusHistoryRepository statusHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private StateWorkflowService service;

    private User applicant;
    private User analyst;

    @BeforeEach
    void setUp() {
        applicant = User.builder()
                .userId("applicant-id")
                .role(tech.nocountry.onboarding.entities.Role.builder()
                        .roleId("ROLE_APPLICANT")
                        .name("APPLICANT")
                        .build())
                .build();
        analyst = User.builder()
                .userId("analyst-id")
                .role(tech.nocountry.onboarding.entities.Role.builder()
                        .roleId("ROLE_ANALYST")
                        .name("ANALYST")
                        .build())
                .build();
    }

    @Test
    void changeStatus_validTransition_analystToUnderReview_ok() {
        CreditApplication app = CreditApplication.builder()
                .applicationId("app-1")
                .status("SUBMITTED")
                .user(applicant)
                .build();

        when(applicationRepository.findByApplicationId("app-1")).thenReturn(Optional.of(app));
        when(userRepository.findByUserId("analyst-id")).thenReturn(Optional.of(analyst));
        when(applicationRepository.save(any(CreditApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        var updated = service.changeStatus("app-1", "UNDER_REVIEW", "analyst-id", "En revisión");

        assertEquals("UNDER_REVIEW", updated.getStatus());
        verify(statusHistoryRepository, times(1)).save(any());
        verify(applicationEventPublisher, times(1)).publishEvent(any());
    }

    @Test
    void changeStatus_invalidTransition_rejectedToApproved_throws() {
        CreditApplication app = CreditApplication.builder()
                .applicationId("app-2")
                .status("REJECTED")
                .build();

        when(applicationRepository.findByApplicationId("app-2")).thenReturn(Optional.of(app));
        when(userRepository.findByUserId("analyst-id")).thenReturn(Optional.of(analyst));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.changeStatus("app-2", "APPROVED", "analyst-id", "No válido")
        );
        assertTrue(ex.getMessage().contains("Transición inválida"));
        verify(statusHistoryRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void changeStatus_rolePermission_applicantCannotApprove_throws() {
        CreditApplication app = CreditApplication.builder()
                .applicationId("app-3")
                .status("SUBMITTED")
                .user(applicant)
                .build();

        when(applicationRepository.findByApplicationId("app-3")).thenReturn(Optional.of(app));
        when(userRepository.findByUserId("applicant-id")).thenReturn(Optional.of(applicant));

        assertThrows(RuntimeException.class, () ->
                service.changeStatus("app-3", "APPROVED", "applicant-id", "Intento no permitido")
        );
        verify(statusHistoryRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void getAllowedTransitions_intersectionByRole_ok() {
        // Para estado UNDER_REVIEW y rol ANALYST, esperados: DOCUMENTS_PENDING, APPROVED, REJECTED
        List<String> allowed = service.getAllowedTransitions("UNDER_REVIEW", "ANALYST");
        assertNotNull(allowed);
        assertTrue(allowed.containsAll(List.of("DOCUMENTS_PENDING", "APPROVED", "REJECTED")));
    }
}
