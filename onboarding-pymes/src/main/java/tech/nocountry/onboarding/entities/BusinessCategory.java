package tech.nocountry.onboarding.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "business_categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BusinessCategory {

    @Id
    @Column(name = "category_id", length = 36)
    @Builder.Default
    private String categoryId = UUID.randomUUID().toString();

    @Column(name = "name", nullable = false, unique = true, length = 70)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @PrePersist
    protected void onCreate() {
        if (categoryId == null) {
            categoryId = UUID.randomUUID().toString();
        }
    }
}

