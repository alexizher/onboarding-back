package tech.nocountry.onboarding.entities;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "address_info")
@Data
public class AdressInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String street;
    private String number;
    private String postalCode;
    private String city;

    private boolean encrypted = true;

}
