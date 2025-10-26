package tech.nocountry.onboarding.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Document {

    @Id
    @Column(name = "document_id", length = 36)
    @Builder.Default
    private String documentId = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, referencedColumnName = "application_id")
    private CreditApplication application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_type_id", nullable = false, referencedColumnName = "document_type_id")
    private DocumentType documentType;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Integer fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "hash", nullable = false, length = 64, unique = true)
    private String hash;

    @Column(name = "verification_status", length = 20)
    @Builder.Default
    private String verificationStatus = "pending";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by", referencedColumnName = "user_id")
    private User verifiedBy;

    @Builder.Default
    @Column(name = "uploaded_at", nullable = false)
    private java.time.LocalDateTime uploadedAt = java.time.LocalDateTime.now();

    @Column(name = "verified_at")
    private java.time.LocalDateTime verifiedAt;

    @PrePersist
    protected void onCreate() {
        if (documentId == null) {
            documentId = UUID.randomUUID().toString();
        }
        if (verificationStatus == null) {
            verificationStatus = "pending";
        }
        if (uploadedAt == null) {
            uploadedAt = java.time.LocalDateTime.now();
        }
    }
}
