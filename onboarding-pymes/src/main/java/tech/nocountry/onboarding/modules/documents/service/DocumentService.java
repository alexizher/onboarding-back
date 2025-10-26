package tech.nocountry.onboarding.modules.documents.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.nocountry.onboarding.entities.Document;
import tech.nocountry.onboarding.entities.DocumentType;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.modules.applications.service.ApplicationService;
import tech.nocountry.onboarding.modules.documents.dto.DocumentResponse;
import tech.nocountry.onboarding.modules.documents.dto.DocumentUploadRequest;
import tech.nocountry.onboarding.repositories.DocumentRepository;
import tech.nocountry.onboarding.repositories.DocumentTypeRepository;
import tech.nocountry.onboarding.repositories.UserRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final UserRepository userRepository;
    private final ApplicationService applicationService;
    
    private static final String UPLOAD_DIR = "uploads/documents/";

    @Transactional
    public DocumentResponse uploadDocument(String userId, DocumentUploadRequest request) {
        log.info("Uploading document for user: {} and application: {}", userId, request.getApplicationId());

        // Validar que el usuario existe
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Validar que el tipo de documento existe
        DocumentType documentType = documentTypeRepository.findById(request.getDocumentTypeId())
                .orElseThrow(() -> new RuntimeException("Tipo de documento no encontrado"));

        // Obtener la aplicación
        tech.nocountry.onboarding.modules.applications.dto.ApplicationResponse application = 
                applicationService.getApplicationById(request.getApplicationId());
        
        // Convertir base64 a bytes
        byte[] fileBytes;
        try {
            fileBytes = Base64.getDecoder().decode(request.getFileContent());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Contenido del archivo inválido (no es base64 válido)");
        }

        // Generar hash del archivo
        String hash = generateFileHash(fileBytes);

        // Guardar archivo en sistema de archivos
        String filePath = saveFile(fileBytes, request.getFileName(), request.getApplicationId());

        // Crear documento
        Document document = Document.builder()
                .application(tech.nocountry.onboarding.entities.CreditApplication.builder()
                        .applicationId(request.getApplicationId())
                        .build())
                .user(user)
                .documentType(documentType)
                .fileName(request.getFileName())
                .filePath(filePath)
                .fileSize(fileBytes.length)
                .mimeType(request.getMimeType())
                .hash(hash)
                .verificationStatus("pending")
                .uploadedAt(LocalDateTime.now())
                .build();

        // Guardar en BD
        Document saved = documentRepository.save(document);
        log.info("Document uploaded with ID: {}", saved.getDocumentId());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(String documentId) {
        log.info("Getting document by ID: {}", documentId);
        
        Document document = documentRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

        return mapToResponse(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByApplication(String applicationId) {
        log.info("Getting documents for application: {}", applicationId);
        
        List<Document> documents = documentRepository.findByApplicationId(applicationId);
        return documents.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getUserDocuments(String userId) {
        log.info("Getting documents for user: {}", userId);
        
        List<Document> documents = documentRepository.findByUserId(userId);
        return documents.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DocumentResponse updateVerificationStatus(String documentId, String status, String verifiedByUserId) {
        log.info("Updating verification status for document: {}", documentId);
        
        Document document = documentRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

        document.setVerificationStatus(status);
        document.setVerifiedAt(LocalDateTime.now());
        
        if (verifiedByUserId != null) {
            User verifier = userRepository.findByUserId(verifiedByUserId)
                    .orElseThrow(() -> new RuntimeException("Usuario verificador no encontrado"));
            document.setVerifiedBy(verifier);
        }

        Document updated = documentRepository.save(document);
        log.info("Document verification status updated: {}", updated.getDocumentId());

        return mapToResponse(updated);
    }

    private String generateFileHash(byte[] fileBytes) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(fileBytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error generating file hash", e);
            return java.util.UUID.randomUUID().toString();
        }
    }

    private String saveFile(byte[] fileBytes, String fileName, String applicationId) {
        try {
            Path uploadDir = Paths.get(UPLOAD_DIR + applicationId);
            Files.createDirectories(uploadDir);
            
            String sanitizedFileName = sanitizeFileName(fileName);
            Path filePath = uploadDir.resolve(sanitizedFileName);
            
            Files.write(filePath, fileBytes);
            
            return filePath.toString();
        } catch (Exception e) {
            log.error("Error saving file", e);
            throw new RuntimeException("Error al guardar el archivo: " + e.getMessage());
        }
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    private DocumentResponse mapToResponse(Document document) {
        return DocumentResponse.builder()
                .documentId(document.getDocumentId())
                .applicationId(document.getApplication().getApplicationId())
                .userId(document.getUser().getUserId())
                .documentTypeId(document.getDocumentType().getDocumentTypeId())
                .documentTypeName(document.getDocumentType().getName())
                .fileName(document.getFileName())
                .filePath(document.getFilePath())
                .fileSize(document.getFileSize())
                .mimeType(document.getMimeType())
                .hash(document.getHash())
                .verificationStatus(document.getVerificationStatus())
                .verifiedBy(document.getVerifiedBy() != null ? document.getVerifiedBy().getUserId() : null)
                .uploadedAt(document.getUploadedAt())
                .verifiedAt(document.getVerifiedAt())
                .build();
    }
}

