package tech.nocountry.onboarding.modules.documents.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tech.nocountry.onboarding.dto.ApiResponse;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.modules.documents.dto.DocumentResponse;
import tech.nocountry.onboarding.modules.documents.dto.DocumentUploadRequest;
import tech.nocountry.onboarding.modules.documents.service.DocumentService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentService documentService;

    /**
     * Subir un documento
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @Valid @RequestBody DocumentUploadRequest request) {
        
        try {
            String userId = getCurrentUserId();
            log.info("Uploading document for user: {}", userId);
            
            DocumentResponse response = documentService.uploadDocument(userId, request);
            
            return ResponseEntity.ok(
                ApiResponse.<DocumentResponse>builder()
                    .success(true)
                    .message("Documento subido exitosamente")
                    .data(response)
                    .build()
            );
            
        } catch (Exception e) {
            log.error("Error uploading document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<DocumentResponse>builder()
                    .success(false)
                    .message("Error al subir el documento: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Obtener un documento por ID
     */
    @GetMapping("/{documentId}")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocument(
            @PathVariable String documentId) {
        
        try {
            log.info("Getting document: {}", documentId);
            
            DocumentResponse response = documentService.getDocumentById(documentId);
            
            return ResponseEntity.ok(
                ApiResponse.<DocumentResponse>builder()
                    .success(true)
                    .message("Documento obtenido exitosamente")
                    .data(response)
                    .build()
            );
            
        } catch (RuntimeException e) {
            log.error("Document not found: {}", documentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<DocumentResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error getting document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<DocumentResponse>builder()
                    .success(false)
                    .message("Error al obtener el documento: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Obtener todos los documentos de una solicitud
     */
    @GetMapping("/application/{applicationId}")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocumentsByApplication(
            @PathVariable String applicationId) {
        
        try {
            log.info("Getting documents for application: {}", applicationId);
            
            List<DocumentResponse> documents = documentService.getDocumentsByApplication(applicationId);
            
            return ResponseEntity.ok(
                ApiResponse.<List<DocumentResponse>>builder()
                    .success(true)
                    .message("Documentos obtenidos exitosamente")
                    .data(documents)
                    .build()
            );
            
        } catch (Exception e) {
            log.error("Error getting documents by application", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<List<DocumentResponse>>builder()
                    .success(false)
                    .message("Error al obtener los documentos: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Obtener todos los documentos del usuario actual
     */
    @GetMapping("/my-documents")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getMyDocuments() {
        
        try {
            String userId = getCurrentUserId();
            log.info("Getting documents for user: {}", userId);
            
            List<DocumentResponse> documents = documentService.getUserDocuments(userId);
            
            return ResponseEntity.ok(
                ApiResponse.<List<DocumentResponse>>builder()
                    .success(true)
                    .message("Documentos obtenidos exitosamente")
                    .data(documents)
                    .build()
            );
            
        } catch (Exception e) {
            log.error("Error getting user documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<List<DocumentResponse>>builder()
                    .success(false)
                    .message("Error al obtener los documentos: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Actualizar el estado de verificación de un documento
     */
    @PutMapping("/{documentId}/verify")
    @PreAuthorize("hasAnyRole('ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DocumentResponse>> verifyDocument(
            @PathVariable String documentId,
            @RequestBody Map<String, String> request) {
        
        try {
            String status = request.get("status");
            String verifiedBy = request.get("verifiedBy");
            
            log.info("Verifying document: {} with status: {}", documentId, status);
            
            DocumentResponse response = documentService.updateVerificationStatus(documentId, status, verifiedBy);
            
            return ResponseEntity.ok(
                ApiResponse.<DocumentResponse>builder()
                    .success(true)
                    .message("Estado de verificación actualizado exitosamente")
                    .data(response)
                    .build()
            );
            
        } catch (RuntimeException e) {
            log.error("Document not found: {}", documentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<DocumentResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error verifying document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<DocumentResponse>builder()
                    .success(false)
                    .message("Error al actualizar el estado: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Descargar un documento
     */
    @GetMapping("/{documentId}/download")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> downloadDocument(@PathVariable String documentId) {
        
        try {
            log.info("Downloading document: {}", documentId);
            
            var fileInfo = documentService.downloadDocument(documentId);
            
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileInfo.getFileName() + "\"")
                    .header("Content-Type", fileInfo.getMimeType())
                    .body(fileInfo.getFileBytes());
            
        } catch (RuntimeException e) {
            log.error("Document not found: {}", documentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error downloading document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.builder()
                    .success(false)
                    .message("Error al descargar el documento: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Eliminar un documento
     */
    @DeleteMapping("/{documentId}")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<ApiResponse<String>> deleteDocument(@PathVariable String documentId) {
        
        try {
            String userId = getCurrentUserId();
            log.info("Deleting document: {} by user: {}", documentId, userId);
            
            documentService.deleteDocument(documentId, userId);
            
            return ResponseEntity.ok(
                ApiResponse.<String>builder()
                    .success(true)
                    .message("Documento eliminado exitosamente")
                    .data("Documento eliminado: " + documentId)
                    .build()
            );
            
        } catch (RuntimeException e) {
            log.error("Document not found or not authorized: {}", documentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<String>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error deleting document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<String>builder()
                    .success(false)
                    .message("Error al eliminar el documento: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Obtener el ID del usuario actual desde el SecurityContext
     */
    private String getCurrentUserId() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            
            if (principal instanceof User) {
                return ((User) principal).getUserId();
            }
            
            throw new IllegalStateException("Usuario no autenticado o formato de autenticación no válido");
        } catch (Exception e) {
            log.error("Error al obtener el ID del usuario actual: {}", e.getMessage());
            throw new IllegalStateException("No se pudo obtener el ID del usuario: " + e.getMessage());
        }
    }
}

