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
        
        // Crear request con todos los campos requeridos
        String validBase64 = "JVBERi0xLjMKJeLjz9MKMyAwIG9iago8PAovVHlwZSAvQ2F0YWxvZwovUGFnZXMgMSAwIFIKPj4KZW5kb2JqCjQgMCBvYmoKPDwKL1R5cGUgL1BhZ2UKL1BhcmVudCAxIDAgUgovUmVzb3VyY2VzIDIgMCBSCi9Db250ZW50cyAzIDAgUgo+PgplbmRvYmoKMiAwIG9iago8PAovUHJvY1NldCBbIC9QREYgL1RleHQgXQovQ29sb3JTcGFjZSA8PAovU3AzIDUgMCBSCj4+Ci9Gb250IDw8Cj4+Cj4+CjMgMCBvYmoKPDwKL0xlbmd0aCA0NQo+PgpzdHJlYW0KQlQKMTIxIDcwNyA1MDAgNzIwIHJlClcKTiAKUQpRCmVuZHN0cmVhbQplbmRvYmoKMSAwIG9iago8PAovVHlwZSAvUGFnZXMKL0NvdW50IDEKL0tpZHMgWyA0IDAgUiBdCj4+CmVuZG9iagp4cmVmCjAgNgowMDAwMDAwMDAwIDY1NTM1IGYgCjAwMDAwMDAzNTUgMDAwMDAgbgowMDAwMDAwMDI1IDAwMDAwIG4KMDAwMDAwMDU2OCAwMDAwMCBuCjAwMDAwMDAxMjIgMDAwMDAgbgowMDAwMDAwNTAwIDAwMDAwIG4KdHJhaWxlcgo8PAovU2l6ZSA2Ci9Sb290IDEgMCBSCj4+CnN0YXJ0eHJlZgo3MjMKJSVFT0Y=";
        
        DocumentUploadRequest request = DocumentUploadRequest.builder()
                .applicationId("app-123")
                .documentTypeId("doc-123")
                .fileName("test.pdf")
                .fileContent(validBase64)
                .mimeType("application/pdf")
                .build();
        
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
        
        // Crear request válido con base64
        String validBase64 = "JVBERi0xLjMKJeLjz9MKMyAwIG9iago8PAovVHlwZSAvQ2F0YWxvZwovUGFnZXMgMSAwIFIKPj4KZW5kb2JqCjQgMCBvYmoKPDwKL1R5cGUgL1BhZ2UKL1BhcmVudCAxIDAgUgovUmVzb3VyY2VzIDIgMCBSCi9Db250ZW50cyAzIDAgUgo+PgplbmRvYmoKMiAwIG9iago8PAovUHJvY1NldCBbIC9QREYgL1RleHQgXQovQ29sb3JTcGFjZSA8PAovU3AzIDUgMCBSCj4+Ci9Gb250IDw8Cj4+Cj4+CjMgMCBvYmoKPDwKL0xlbmd0aCA0NQo+PgpzdHJlYW0KQlQKMTIxIDcwNyA1MDAgNzIwIHJlClcKTiAKUQpRCmVuZHN0cmVhbQplbmRvYmoKMSAwIG9iago8PAovVHlwZSAvUGFnZXMKL0NvdW50IDEKL0tpZHMgWyA0IDAgUiBdCj4+CmVuZG9iagp4cmVmCjAgNgowMDAwMDAwMDAwIDY1NTM1IGYgCjAwMDAwMDAzNTUgMDAwMDAgbgowMDAwMDAwMDI1IDAwMDAwIG4KMDAwMDAwMDU2OCAwMDAwMCBuCjAwMDAwMDAxMjIgMDAwMDAgbgowMDAwMDAwNTAwIDAwMDAwIG4KdHJhaWxlcgo8PAovU2l6ZSA2Ci9Sb290IDEgMCBSCj4+CnN0YXJ0eHJlZgo3MjMKJSVFT0Y=";
        
        DocumentUploadRequest request = DocumentUploadRequest.builder()
                .applicationId("app-123")
                .documentTypeId("doc-123")
                .fileName("test.pdf")
                .fileContent(validBase64)
                .mimeType("application/pdf")
                .build();
        
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

