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
import tech.nocountry.onboarding.modules.documents.controller.DocumentController;
import tech.nocountry.onboarding.modules.documents.dto.DocumentResponse;
import tech.nocountry.onboarding.modules.documents.dto.DocumentUploadRequest;
import tech.nocountry.onboarding.modules.documents.service.DocumentService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

    @Mock
    private DocumentService documentService;

    @InjectMocks
    private DocumentController controller;

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
    void testUploadDocument_Success() throws Exception {
        // Arrange
        setupSecurityContext();
        
        DocumentUploadRequest request = new DocumentUploadRequest();
        request.setApplicationId("app-123");
        request.setDocumentTypeId("doc-123");
        request.setFileName("test.pdf");
        
        DocumentResponse response = DocumentResponse.builder()
                .documentId("doc-123")
                .fileName("test.pdf")
                .verificationStatus("PENDING")
                .build();

        when(documentService.uploadDocument(anyString(), any(DocumentUploadRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/documents/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Documento subido exitosamente"))
                .andExpect(jsonPath("$.data.documentId").value("doc-123"));

        verify(documentService, times(1)).uploadDocument(anyString(), any(DocumentUploadRequest.class));
    }

    @Test
    void testUploadDocument_ServiceThrowsException() throws Exception {
        // Arrange
        setupSecurityContext();
        
        DocumentUploadRequest request = new DocumentUploadRequest();
        
        when(documentService.uploadDocument(anyString(), any(DocumentUploadRequest.class)))
                .thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        mockMvc.perform(post("/api/documents/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));

        verify(documentService, times(1)).uploadDocument(anyString(), any(DocumentUploadRequest.class));
    }

    @Test
    void testGetDocument_Success() throws Exception {
        // Arrange
        DocumentResponse response = DocumentResponse.builder()
                .documentId("doc-123")
                .fileName("test.pdf")
                .verificationStatus("APPROVED")
                .build();

        when(documentService.getDocumentById("doc-123"))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/documents/doc-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Documento obtenido exitosamente"))
                .andExpect(jsonPath("$.data.documentId").value("doc-123"));

        verify(documentService, times(1)).getDocumentById("doc-123");
    }

}

