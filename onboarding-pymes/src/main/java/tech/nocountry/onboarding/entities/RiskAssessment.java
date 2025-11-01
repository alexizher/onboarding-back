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
@Table(name = "risk_assessments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RiskAssessment {

    @Id
    @Column(name = "assessment_id", length = 36)
    @Builder.Default
    private String assessmentId = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, referencedColumnName = "application_id")
    private CreditApplication application;

    @Column(name = "score", nullable = false)
    private Float score; // Score de 0 a 100

    @Column(name = "level", nullable = false, length = 20)
    private String level; // LOW, MEDIUM, HIGH, VERY_HIGH

    @Column(name = "details", columnDefinition = "JSON")
    private String details; // JSON con detalles del cálculo

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessed_by", referencedColumnName = "user_id")
    private User assessedBy;

    @Column(name = "assessed_at", nullable = false)
    @Builder.Default
    private LocalDateTime assessedAt = LocalDateTime.now();

    @Column(name = "is_automated", nullable = false)
    @Builder.Default
    private Boolean isAutomated = true;

    @PrePersist
    protected void onCreate() {
        if (assessmentId == null) {
            assessmentId = UUID.randomUUID().toString();
        }
        if (assessedAt == null) {
            assessedAt = LocalDateTime.now();
        }
        if (isAutomated == null) {
            isAutomated = true;
        }
    }
}

