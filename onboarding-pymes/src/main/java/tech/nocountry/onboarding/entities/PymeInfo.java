package tech.nocountry.onboarding.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

// @Entity - DESHABILITADO: Esta entidad no se usa en el sistema actual
// @Table(name = "pyme_info")
@Data
public class PymeInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String rubro;

    @Column(nullable = false)
    private boolean creditosAnteriores;

    private boolean encrypted = true;
}
