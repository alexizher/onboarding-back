package tech.nocountry.onboarding.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "credit_applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CreditApplication {

    @Id
    @Column(name = "application_id", length = 36)
    @Builder.Default
    private String applicationId = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", referencedColumnName = "category_id")
    private BusinessCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profession_id", referencedColumnName = "profession_id")
    private Profession profession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id", referencedColumnName = "destination_id")
    private CreditDestination destination;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "state_id", referencedColumnName = "state_id")
    private ApplicationState state;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", referencedColumnName = "city_id")
    private City city;

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "pending";

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "cuit", nullable = false, length = 20)
    private String cuit;

    @Column(name = "company_address", length = 255)
    private String companyAddress;

    @Column(name = "amount_requested", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountRequested;

    @Column(name = "purpose", columnDefinition = "TEXT")
    private String purpose;

    @Column(name = "credit_months")
    private Integer creditMonths;

    @Column(name = "monthly_income", precision = 15, scale = 2)
    private BigDecimal monthlyIncome;

    @Column(name = "monthly_expenses", precision = 15, scale = 2)
    private BigDecimal monthlyExpenses;

    @Column(name = "existing_debt", precision = 15, scale = 2)
    private BigDecimal existingDebt;

    @Column(name = "accept_terms")
    @Builder.Default
    private Boolean acceptTerms = false;

    @Column(name = "form_data", columnDefinition = "JSON")
    private String formData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to", referencedColumnName = "user_id")
    private User assignedTo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (applicationId == null) {
            applicationId = UUID.randomUUID().toString();
        }
        if (status == null) {
            status = "pending";
        }
        if (acceptTerms == null) {
            acceptTerms = false;
        }
    }
}

