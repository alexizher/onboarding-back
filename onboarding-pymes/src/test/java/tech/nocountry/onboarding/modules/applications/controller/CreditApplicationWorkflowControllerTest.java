package tech.nocountry.onboarding.modules.applications.controller;

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
import tech.nocountry.onboarding.modules.applications.dto.ApplicationResponse;
import tech.nocountry.onboarding.modules.applications.dto.StatusChangeRequest;
import tech.nocountry.onboarding.modules.applications.dto.StatusHistoryResponse;
import tech.nocountry.onboarding.modules.applications.service.ApplicationService;
import tech.nocountry.onboarding.modules.applications.service.StateWorkflowService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CreditApplicationWorkflowControllerTest {

    @Mock
    private ApplicationService applicationService;

    @Mock
    private StateWorkflowService stateWorkflowService;

    @InjectMocks
    private CreditApplicationController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User analystUser;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();

        Role analystRole = Role.builder().name("ANALYST").build();
        analystUser = User.builder()
                .userId("analyst-1")
                .role(analystRole)
                .build();

        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(analystUser);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void changeStatus_success_returnsOk() throws Exception {
        // Arrange
        StatusChangeRequest req = StatusChangeRequest.builder()
                .newStatus("UNDER_REVIEW")
                .comments("Enviar a revisión")
                .build();

        ApplicationResponse appResp = ApplicationResponse.builder()
                .applicationId("app-1")
                .status("UNDER_REVIEW")
                .build();

        doReturn(appResp) // getApplicationById returns response
                .when(applicationService).getApplicationById("app-1");

        // Act & Assert
        mockMvc.perform(put("/api/applications/app-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Estado actualizado exitosamente"))
                .andExpect(jsonPath("$.data.applicationId").value("app-1"));

        verify(stateWorkflowService, times(1))
                .changeStatus(eq("app-1"), eq("UNDER_REVIEW"), eq("analyst-1"), anyString());
    }

    @Test
    void changeStatus_invalidTransition_returnsBadRequest() throws Exception {
        // Arrange
        StatusChangeRequest req = StatusChangeRequest.builder()
                .newStatus("APPROVED")
                .comments("Aprobar")
                .build();

        doThrow(new RuntimeException("Transición inválida"))
                .when(stateWorkflowService).changeStatus(anyString(), anyString(), anyString(), anyString());

        // Act & Assert
        mockMvc.perform(put("/api/applications/app-2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getStatusHistory_success_returnsOk() throws Exception {
        // Arrange
        List<StatusHistoryResponse> history = List.of(
                StatusHistoryResponse.builder().historyId("h1").newStatus("UNDER_REVIEW").build(),
                StatusHistoryResponse.builder().historyId("h2").newStatus("APPROVED").build()
        );
        when(stateWorkflowService.getStatusHistory("app-3")).thenReturn(history);

        // Act & Assert
        mockMvc.perform(get("/api/applications/app-3/status-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].newStatus").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.data[1].newStatus").value("APPROVED"));
    }

    @Test
    void getAllowedTransitions_success_returnsOk() throws Exception {
        // Arrange
        ApplicationResponse appResp = ApplicationResponse.builder()
                .applicationId("app-4")
                .status("UNDER_REVIEW")
                .build();
        when(applicationService.getApplicationById("app-4")).thenReturn(appResp);
        when(stateWorkflowService.getAllowedTransitions("UNDER_REVIEW", "ANALYST"))
                .thenReturn(List.of("DOCUMENTS_PENDING", "APPROVED", "REJECTED"));

        // Act & Assert
        mockMvc.perform(get("/api/applications/app-4/allowed-transitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0]").value("DOCUMENTS_PENDING"));
    }
}
