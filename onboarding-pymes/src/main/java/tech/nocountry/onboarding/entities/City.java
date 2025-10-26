package tech.nocountry.onboarding.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "cities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class City {

    @Id
    @Column(name = "city_id", length = 36)
    @Builder.Default
    private String cityId = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false, referencedColumnName = "department_id")
    private Department department;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "code", length = 10)
    private String code;

    @PrePersist
    protected void onCreate() {
        if (cityId == null) {
            cityId = UUID.randomUUID().toString();
        }
    }
}

