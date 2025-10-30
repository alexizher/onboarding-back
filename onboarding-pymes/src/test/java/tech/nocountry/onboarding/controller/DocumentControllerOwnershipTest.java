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
import tech.nocountry.onboarding.modules.documents.dto.DocumentResponse;
import tech.nocountry.onboarding.modules.documents.dto.DocumentUploadRequest;
import tech.nocountry.onboarding.modules.documents.dto.FileInfo;
import tech.nocountry.onboarding.modules.documents.service.DocumentService;

import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DocumentControllerOwnershipTest {

    @Mock
    private DocumentService documentService;

    @InjectMocks
    private tech.nocountry.onboarding.modules.documents.controller.DocumentController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private User applicantUser;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();

        Role applicantRole = Role.builder().name("APPLICANT").build();
        applicantUser = User.builder().userId("applicant-1").role(applicantRole).build();

        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(applicantUser);
        SecurityContext securityContext = org.mockito.Mockito.mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void uploadDocument_duplicate_returnsServerErrorWithMessage() throws Exception {
        byte[] content = "hello".getBytes();
        String b64 = Base64.getEncoder().encodeToString(content);

        DocumentUploadRequest req = DocumentUploadRequest.builder()
                .applicationId("app-1")
                .documentTypeId("dt-1")
                .fileName("dup.pdf")
                .fileContent(b64)
                .mimeType("application/pdf")
                .build();

        when(documentService.uploadDocument(anyString(), any(DocumentUploadRequest.class)))
                .thenThrow(new RuntimeException("Documento duplicado"));

        mockMvc.perform(post("/api/documents/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void download_asApplicant_notOwner_forbidden() throws Exception {
        // Controller checks ownership using getDocumentById before downloading
        DocumentResponse doc = DocumentResponse.builder()
                .documentId("doc-1")
                .userId("another-user")
                .applicationId("app-1")
                .fileName("x.pdf")
                .build();
        when(documentService.getDocumentById("doc-1")).thenReturn(doc);

        mockMvc.perform(get("/api/documents/doc-1/download"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void download_asApplicant_owner_ok() throws Exception {
        DocumentResponse doc = DocumentResponse.builder()
                .documentId("doc-2")
                .userId("applicant-1")
                .applicationId("app-1")
                .fileName("ok.pdf")
                .mimeType("application/pdf")
                .build();
        when(documentService.getDocumentById("doc-2")).thenReturn(doc);
        when(documentService.downloadDocument("doc-2")).thenReturn(
                FileInfo.builder().fileName("ok.pdf").mimeType("application/pdf").fileBytes("a".getBytes()).build()
        );

        mockMvc.perform(get("/api/documents/doc-2/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("filename=\"ok.pdf\"")));
    }
}


