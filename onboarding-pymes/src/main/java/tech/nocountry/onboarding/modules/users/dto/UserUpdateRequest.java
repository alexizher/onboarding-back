package tech.nocountry.onboarding.modules.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    @Size(max = 100, message = "El nombre completo no puede exceder 100 caracteres")
    private String fullName;

    @Size(max = 20, message = "El DNI no puede exceder 20 caracteres")
    private String dni;

    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    private String phone;

    @Email(message = "El email debe tener un formato válido")
    @Size(max = 100, message = "El email no puede exceder 100 caracteres")
    private String email;

    private Boolean isActive;
    private Boolean consentGdpr;
    private String roleId; // Solo admin puede cambiar esto
}

