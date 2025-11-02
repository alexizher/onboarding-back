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
public class BusinessCategoryRequest {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 70, message = "El nombre no puede exceder 70 caracteres")
    private String name;

    @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
    private String description;

    @Size(max = 20, message = "El nivel de riesgo no puede exceder 20 caracteres")
    private String riskLevel; // LOW, MEDIUM, HIGH
}

