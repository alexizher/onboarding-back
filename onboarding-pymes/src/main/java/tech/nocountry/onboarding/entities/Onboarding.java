package tech.nocountry.onboarding.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

// @Entity - DESHABILITADO: Esta entidad no se usa en el sistema actual
// @Table(name = "onboarding")
@Data
public class Onboarding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private int stepCompleted = 1;

    private boolean isComplete = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();
}
