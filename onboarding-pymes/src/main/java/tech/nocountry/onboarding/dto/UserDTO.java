
package tech.nocountry.onboarding.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import tech.nocountry.onboarding.entities.User;

@Getter
@Setter
public class UserDTO {
    private String id;
    private String email;
    private String phone;
    private String fullName;
    private String roleName; // Solo el nombre del rol, no el objeto completo
    private LocalDateTime createdAt;
    
    // Constructor que recibe User entity
    public UserDTO(User user) {
        this.id = user.getUserId();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.fullName = user.getFullName();
        this.roleName = user.getRole() != null ? user.getRole().getName() : null;
        this.createdAt = user.getCreatedAt();
    }
}
