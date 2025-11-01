package tech.nocountry.onboarding.modules.documents.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentFilterRequest {
    
    // Filtros
    private String verificationStatus;  // pending, verified, rejected
    private String documentTypeId;
    private String applicationId;
    private String userId;  // Usuario que subió el documento
    private String verifiedByUserId;  // Usuario que verificó el documento
    private String fileName;
    
    // Filtros de fecha
    private LocalDateTime uploadedFrom;
    private LocalDateTime uploadedTo;
    private LocalDateTime verifiedFrom;
    private LocalDateTime verifiedTo;
    
    // Paginación
    @Builder.Default
    private Integer page = 0;
    @Builder.Default
    private Integer size = 20;
    
    // Ordenamiento
    @Builder.Default
    private String sortBy = "uploadedAt";  // uploadedAt, verifiedAt, fileName, verificationStatus
    @Builder.Default
    private String sortDirection = "DESC";  // ASC, DESC
}

