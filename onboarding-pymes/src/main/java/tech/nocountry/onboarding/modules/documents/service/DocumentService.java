package tech.nocountry.onboarding.modules.documents.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.nocountry.onboarding.entities.Document;
import tech.nocountry.onboarding.entities.DocumentType;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.modules.applications.service.ApplicationService;
import tech.nocountry.onboarding.modules.documents.dto.*;
import tech.nocountry.onboarding.modules.documents.events.DocumentVerifiedEvent;
import tech.nocountry.onboarding.repositories.DocumentRepository;
import tech.nocountry.onboarding.repositories.DocumentTypeRepository;
import tech.nocountry.onboarding.repositories.UserRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final UserRepository userRepository;
    private final ApplicationService applicationService;
    private final tech.nocountry.onboarding.services.AuditLogService auditLogService;
    private final ApplicationEventPublisher applicationEventPublisher;
    
    private static final String UPLOAD_DIR = "uploads/documents/";
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/jpg"
    );

    @Transactional
    public DocumentResponse uploadDocument(String userId, DocumentUploadRequest request) {
        log.info("Uploading document for user: {} and application: {}", userId, request.getApplicationId());

        // Validar que el usuario existe
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Validar que el tipo de documento existe
        DocumentType documentType = documentTypeRepository.findById(request.getDocumentTypeId())
                .orElseThrow(() -> new RuntimeException("Tipo de documento no encontrado"));

        // Validar que la aplicación existe
        applicationService.getApplicationById(request.getApplicationId());
        
        // Convertir base64 a bytes
        byte[] fileBytes;
        try {
            fileBytes = Base64.getDecoder().decode(request.getFileContent());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Contenido del archivo inválido (no es base64 válido)");
        }
        
        // Validar archivo
        validateFile(fileBytes, request.getMimeType());

        // Generar hash del archivo
        String hash = generateFileHash(fileBytes);

        // Validar duplicados por hash
        if (documentRepository.existsByHash(hash)) {
            throw new RuntimeException("Documento duplicado: ya existe un archivo con el mismo contenido");
        }

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
        auditLogService.record(userId, "DOCUMENT_UPLOAD", null,
                "Subida documento " + saved.getDocumentId() + " tipo=" + documentType.getName());

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
        auditLogService.record(verifiedByUserId, "DOCUMENT_VERIFY", null,
                "Verificación documento " + documentId + " -> " + status);

        // Obtener userId del dueño del documento (para notificación SSE)
        String documentOwnerUserId = null;
        try {
            if (updated.getUser() != null) {
                documentOwnerUserId = updated.getUser().getUserId();
            }
        } catch (Exception e) {
            log.warn("Error al acceder al user del documento {}: {}", documentId, e.getMessage());
        }

        // Obtener applicationId
        String applicationId = null;
        try {
            if (updated.getApplication() != null) {
                applicationId = updated.getApplication().getApplicationId();
            }
        } catch (Exception e) {
            log.warn("Error al acceder al application del documento {}: {}", documentId, e.getMessage());
        }

        // Publicar evento de verificación de documento para notificaciones SSE
        if (documentOwnerUserId != null && applicationId != null) {
            try {
                applicationEventPublisher.publishEvent(new DocumentVerifiedEvent(
                        this,
                        documentId,
                        applicationId,
                        documentOwnerUserId,
                        status,
                        verifiedByUserId
                ));
                log.info("DocumentVerifiedEvent publicado - documento: {}, usuario: {}, estado: {}", 
                         documentId, documentOwnerUserId, status);
            } catch (Exception e) {
                log.error("Error al publicar evento de verificación de documento: {}", e.getMessage(), e);
            }
        }

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

    @Transactional(readOnly = true)
    public FileInfo downloadDocument(String documentId) {
        log.info("Downloading document by ID: {}", documentId);
        
        Document document = documentRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

        try {
            Path filePath = Paths.get(document.getFilePath());
            byte[] fileBytes = Files.readAllBytes(filePath);
            auditLogService.record(document.getUser() != null ? document.getUser().getUserId() : null,
                    "DOCUMENT_DOWNLOAD", null, "Descarga documento " + documentId);
            
            return FileInfo.builder()
                    .fileName(document.getFileName())
                    .fileBytes(fileBytes)
                    .mimeType(document.getMimeType())
                    .build();
        } catch (Exception e) {
            log.error("Error reading file from disk", e);
            throw new RuntimeException("Error al leer el archivo del disco: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteDocument(String documentId, String userId) {
        log.info("Deleting document: {} by user: {}", documentId, userId);
        
        Document document = documentRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

        // Validar que el usuario sea el propietario del documento
        if (!document.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("No tiene permiso para eliminar este documento");
        }

        // Eliminar archivo del disco
        try {
            Path filePath = Paths.get(document.getFilePath());
            Files.deleteIfExists(filePath);
            log.info("File deleted from disk: {}", filePath);
        } catch (Exception e) {
            log.error("Error deleting file from disk", e);
        }

        // Eliminar de la base de datos
        documentRepository.delete(document);
        log.info("Document deleted from database: {}", documentId);
        auditLogService.record(userId, "DOCUMENT_DELETE", null,
                "Eliminación documento " + documentId);
    }

    private void validateFile(byte[] fileBytes, String mimeType) {
        // Validar tamaño
        if (fileBytes.length > MAX_FILE_SIZE) {
            throw new RuntimeException("Archivo muy grande. Máximo permitido: 10MB");
        }

        // Validar tipo MIME
        if (mimeType != null && !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new RuntimeException("Tipo de archivo no permitido. Solo se permiten: PDF, JPEG, PNG");
        }
    }

    private DocumentResponse mapToResponse(Document document) {
        // Manejar lazy loading del application
        String applicationId = null;
        try {
            if (document.getApplication() != null) {
                applicationId = document.getApplication().getApplicationId();
            }
        } catch (Exception e) {
            log.warn("Error al acceder al application del documento {}: {}", document.getDocumentId(), e.getMessage());
        }
        
        // Manejar lazy loading del user
        String userId = null;
        try {
            if (document.getUser() != null) {
                userId = document.getUser().getUserId();
            }
        } catch (Exception e) {
            log.warn("Error al acceder al user del documento {}: {}", document.getDocumentId(), e.getMessage());
        }
        
        // Manejar lazy loading del documentType
        String documentTypeId = null;
        String documentTypeName = null;
        try {
            if (document.getDocumentType() != null) {
                documentTypeId = document.getDocumentType().getDocumentTypeId();
                documentTypeName = document.getDocumentType().getName();
            }
        } catch (Exception e) {
            log.warn("Error al acceder al documentType del documento {}: {}", document.getDocumentId(), e.getMessage());
        }
        
        // Manejar lazy loading del verifiedBy
        String verifiedByUserId = null;
        try {
            if (document.getVerifiedBy() != null) {
                verifiedByUserId = document.getVerifiedBy().getUserId();
            }
        } catch (Exception e) {
            log.warn("Error al acceder al verifiedBy del documento {}: {}", document.getDocumentId(), e.getMessage());
        }
        
        return DocumentResponse.builder()
                .documentId(document.getDocumentId())
                .applicationId(applicationId)
                .userId(userId)
                .documentTypeId(documentTypeId)
                .documentTypeName(documentTypeName)
                .fileName(document.getFileName())
                .filePath(document.getFilePath())
                .fileSize(document.getFileSize())
                .mimeType(document.getMimeType())
                .hash(document.getHash())
                .verificationStatus(document.getVerificationStatus())
                .verifiedBy(verifiedByUserId)
                .uploadedAt(document.getUploadedAt())
                .verifiedAt(document.getVerifiedAt())
                .build();
    }

    public List<DocumentType> getAllDocumentTypes() {
        return documentTypeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public PagedDocumentResponse filterDocuments(DocumentFilterRequest filter) {
        log.info("Filtering documents with request: {}", filter);
        
        // Crear Pageable con ordenamiento
        Sort sort = Sort.by(
            "DESC".equalsIgnoreCase(filter.getSortDirection()) 
                ? Sort.Direction.DESC 
                : Sort.Direction.ASC,
            filter.getSortBy()
        );
        
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);
        
        // Llamar al método del repositorio con filtros
        Page<Document> page = documentRepository.findWithFilters(
            filter.getVerificationStatus(),
            filter.getDocumentTypeId(),
            filter.getApplicationId(),
            filter.getUserId(),
            filter.getVerifiedByUserId(),
            filter.getFileName(),
            filter.getUploadedFrom(),
            filter.getUploadedTo(),
            filter.getVerifiedFrom(),
            filter.getVerifiedTo(),
            pageable
        );
        
        // Convertir a PagedDocumentResponse
        List<DocumentResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        return PagedDocumentResponse.builder()
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

    @Transactional(readOnly = true)
    public PagedDocumentResponse getPendingDocuments(Integer page, Integer size) {
        log.info("Getting pending documents - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 20, 
                Sort.by(Sort.Direction.DESC, "uploadedAt"));
        
        Page<Document> documentPage = documentRepository.findPendingDocuments(pageable);
        
        List<DocumentResponse> content = documentPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        return PagedDocumentResponse.builder()
                .content(content)
                .page(documentPage.getNumber())
                .size(documentPage.getSize())
                .totalElements(documentPage.getTotalElements())
                .totalPages(documentPage.getTotalPages())
                .first(documentPage.isFirst())
                .last(documentPage.isLast())
                .hasNext(documentPage.hasNext())
                .hasPrevious(documentPage.hasPrevious())
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDocumentStatistics() {
        log.info("Getting document statistics");
        
        Map<String, Object> stats = new HashMap<>();
        
        // Contar por estado de verificación
        long pending = documentRepository.countByVerificationStatus("pending");
        long verified = documentRepository.countByVerificationStatus("verified");
        long rejected = documentRepository.countByVerificationStatus("rejected");
        long total = documentRepository.count();
        
        stats.put("total", total);
        stats.put("pending", pending);
        stats.put("verified", verified);
        stats.put("rejected", rejected);
        
        // Porcentajes
        if (total > 0) {
            stats.put("pendingPercentage", (double) pending / total * 100);
            stats.put("verifiedPercentage", (double) verified / total * 100);
            stats.put("rejectedPercentage", (double) rejected / total * 100);
        } else {
            stats.put("pendingPercentage", 0.0);
            stats.put("verifiedPercentage", 0.0);
            stats.put("rejectedPercentage", 0.0);
        }
        
        // Documentos por tipo
        Map<String, Long> byType = documentRepository.findAll()
                .stream()
                .filter(doc -> doc.getDocumentType() != null)
                .collect(Collectors.groupingBy(
                    doc -> {
                        try {
                            return doc.getDocumentType().getName();
                        } catch (Exception e) {
                            return "unknown";
                        }
                    },
                    Collectors.counting()
                ));
        stats.put("byType", byType);
        
        // Documentos subidos hoy y en el último mes
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1);
        
        long uploadedLastMonth = documentRepository.findAll()
                .stream()
                .filter(doc -> doc.getUploadedAt() != null && doc.getUploadedAt().isAfter(oneMonthAgo))
                .count();
        
        long uploadedToday = documentRepository.findAll()
                .stream()
                .filter(doc -> doc.getUploadedAt() != null && 
                              doc.getUploadedAt().isAfter(today) && 
                              doc.getUploadedAt().isBefore(tomorrow))
                .count();
        
        stats.put("uploadedLastMonth", uploadedLastMonth);
        stats.put("uploadedToday", uploadedToday);
        
        log.info("Statistics generated: total={}, pending={}, verified={}, rejected={}", 
                 total, pending, verified, rejected);
        
        return stats;
    }
}

