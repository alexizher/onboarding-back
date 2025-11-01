package tech.nocountry.onboarding.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "kyc_verifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class KycVerification {

    @Id
    @Column(name = "verification_id", length = 36)
    @Builder.Default
    private String verificationId = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, referencedColumnName = "application_id")
    private CreditApplication application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "user_id")
    private User user;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider; // DataCrédito, Mock, etc.

    @Column(name = "verification_type", nullable = false, length = 50)
    private String verificationType; // IDENTITY, BIOMETRIC, DOCUMENT, etc.

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "pending"; // pending, verified, rejected, failed

    @Column(name = "result", columnDefinition = "JSON")
    private String result; // JSON con detalles del resultado

    @Column(name = "score")
    private Float score; // Score de confianza (0-100)

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (verificationId == null) {
            verificationId = UUID.randomUUID().toString();
        }
        if (status == null) {
            status = "pending";
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

