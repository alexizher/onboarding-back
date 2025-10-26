package tech.nocountry.onboarding.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "application_states")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ApplicationState {

    @Id
    @Column(name = "state_id", length = 36)
    @Builder.Default
    private String stateId = UUID.randomUUID().toString();

    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "step_order")
    private Integer stepOrder;

    @PrePersist
    protected void onCreate() {
        if (stateId == null) {
            stateId = UUID.randomUUID().toString();
        }
    }
}

