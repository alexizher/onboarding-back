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
public class DocumentResponse {

    private String documentId;
    private String applicationId;
    private String userId;
    private String documentTypeId;
    private String documentTypeName;
    private String fileName;
    private String filePath;
    private Integer fileSize;
    private String mimeType;
    private String hash;
    private String verificationStatus;
    private String verifiedBy;
    private LocalDateTime uploadedAt;
    private LocalDateTime verifiedAt;
}

