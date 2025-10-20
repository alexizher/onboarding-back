package tech.nocountry.onboarding.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {
    
    @NotBlank(message = "Email es requerido")
    @Email(message = "Email debe tener un formato válido")
    private String email;
    
    @NotBlank(message = "Password es requerido")
    private String password;
}
