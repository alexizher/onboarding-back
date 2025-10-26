package tech.nocountry.onboarding.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "departments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Department {

    @Id
    @Column(name = "department_id", length = 36)
    @Builder.Default
    private String departmentId = UUID.randomUUID().toString();

    @Column(name = "name", nullable = false, unique = true, length = 70)
    private String name;

    @Column(name = "code", length = 10)
    private String code;

    @PrePersist
    protected void onCreate() {
        if (departmentId == null) {
            departmentId = UUID.randomUUID().toString();
        }
    }
}

