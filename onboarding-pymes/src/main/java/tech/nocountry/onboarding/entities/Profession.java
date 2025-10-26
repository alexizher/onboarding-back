package tech.nocountry.onboarding.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "professions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Profession {

    @Id
    @Column(name = "profession_id", length = 36)
    @Builder.Default
    private String professionId = UUID.randomUUID().toString();

    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @PrePersist
    protected void onCreate() {
        if (professionId == null) {
            professionId = UUID.randomUUID().toString();
        }
    }
}

