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
public class CityRequest {
    @NotBlank(message = "El ID del departamento es obligatorio")
    private String departmentId;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 255, message = "El nombre no puede exceder 255 caracteres")
    private String name;

    @Size(max = 10, message = "El código no puede exceder 10 caracteres")
    private String code;
}

