package tech.nocountry.onboarding.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "document_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DocumentType {

    @Id
    @Column(name = "document_type_id", length = 36)
    @Builder.Default
    private String documentTypeId = UUID.randomUUID().toString();

    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "is_required")
    @Builder.Default
    private Boolean isRequired = false;

    @PrePersist
    protected void onCreate() {
        if (documentTypeId == null) {
            documentTypeId = UUID.randomUUID().toString();
        }
        if (isRequired == null) {
            isRequired = false;
        }
    }
}

