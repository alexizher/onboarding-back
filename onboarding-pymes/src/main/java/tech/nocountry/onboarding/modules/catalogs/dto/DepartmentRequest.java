package tech.nocountry.onboarding.modules.catalogs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentRequest {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 70, message = "El nombre no puede exceder 70 caracteres")
    private String name;

    @Size(max = 10, message = "El código no puede exceder 10 caracteres")
    private String code;
}

