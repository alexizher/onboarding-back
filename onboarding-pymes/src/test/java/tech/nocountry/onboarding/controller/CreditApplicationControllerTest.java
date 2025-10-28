package tech.nocountry.onboarding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tech.nocountry.onboarding.entities.Role;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.modules.applications.controller.CreditApplicationController;
import tech.nocountry.onboarding.modules.applications.dto.ApplicationRequest;
import tech.nocountry.onboarding.modules.applications.dto.ApplicationResponse;
import tech.nocountry.onboarding.modules.applications.service.ApplicationService;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CreditApplicationControllerTest {

    @Mock
    private ApplicationService applicationService;

    @InjectMocks
    private CreditApplicationController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User testUser;
    private Role testRole;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        
        // Configurar usuario de prueba
        testRole = Role.builder()
                .roleId("ROLE_APPLICANT")
                .build();
        
        testUser = User.builder()
                .userId("test-user-123")
                .username("testuser")
                .email("test@test.com")
                .fullName("Test User")
                .role(testRole)
                .build();
    }

    private void setupSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(testUser);
        
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testCreateApplication_Success() throws Exception {
        // Arrange
        setupSecurityContext();
        
        ApplicationRequest request = ApplicationRequest.builder()
                .companyName("Test Company")
                .cuit("12345678901")
                .amountRequested(new BigDecimal("10000"))
                .purpose("Test purpose")
                .creditMonths(12)
                .monthlyIncome(new BigDecimal("5000"))
                .monthlyExpenses(new BigDecimal("3000"))
                .existingDebt(new BigDecimal("1000"))
                .acceptTerms(true)
                .build();
        
        ApplicationResponse response = ApplicationResponse.builder()
                .applicationId("app-123")
                .amountRequested(new BigDecimal("10000"))
                .status("PENDING")
                .build();

        when(applicationService.createApplication(anyString(), any(ApplicationRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Solicitud de crédito creada exitosamente"))
                .andExpect(jsonPath("$.data.applicationId").value("app-123"));

        verify(applicationService, times(1)).createApplication(anyString(), any(ApplicationRequest.class));
    }

    @Test
    void testCreateApplication_ServiceThrowsException() throws Exception {
        // Arrange
        setupSecurityContext();
        
        ApplicationRequest request = ApplicationRequest.builder()
                .companyName("Test Company")
                .cuit("12345678901")
                .amountRequested(new BigDecimal("10000"))
                .acceptTerms(true)
                .build();
        
        when(applicationService.createApplication(anyString(), any(ApplicationRequest.class)))
                .thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));

        verify(applicationService, times(1)).createApplication(anyString(), any(ApplicationRequest.class));
    }

    @Test
    void testGetApplication_Success() throws Exception {
        // Arrange
        ApplicationResponse response = ApplicationResponse.builder()
                .applicationId("app-123")
                .amountRequested(new BigDecimal("10000"))
                .status("PENDING")
                .build();

        when(applicationService.getApplicationById("app-123"))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/applications/app-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Solicitud obtenida exitosamente"))
                .andExpect(jsonPath("$.data.applicationId").value("app-123"));

        verify(applicationService, times(1)).getApplicationById("app-123");
    }

    @Test
    void testGetApplication_NotFound() throws Exception {
        // Arrange
        when(applicationService.getApplicationById("non-existent"))
                .thenThrow(new RuntimeException("Application not found"));

        // Act & Assert
        mockMvc.perform(get("/api/applications/non-existent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        verify(applicationService, times(1)).getApplicationById("non-existent");
    }


    @Test
    void testDeleteApplication_Success() throws Exception {
        // Arrange
        doNothing().when(applicationService).deleteApplication("app-123");

        // Act & Assert
        mockMvc.perform(delete("/api/applications/app-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Solicitud eliminada exitosamente"));

        verify(applicationService, times(1)).deleteApplication("app-123");
    }

    @Test
    void testDeleteApplication_NotFound() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Application not found"))
                .when(applicationService).deleteApplication("non-existent");

        // Act & Assert
        mockMvc.perform(delete("/api/applications/non-existent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        verify(applicationService, times(1)).deleteApplication("non-existent");
    }
}

