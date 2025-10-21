package tech.nocountry.onboarding.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @Column(name = "role_id", length = 50)
    private String roleId; // ROLE_USER, ROLE_ADMIN, ROLE_MODERATOR

    @Column(name = "name", nullable = false, length = 100)
    private String name; // User, Admin, Moderator

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "permissions", columnDefinition = "JSON")
    private String permissions; // JSON string con los permisos

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
