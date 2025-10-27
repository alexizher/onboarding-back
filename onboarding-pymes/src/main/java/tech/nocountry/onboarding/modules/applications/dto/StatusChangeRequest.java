package tech.nocountry.onboarding.modules.applications.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusChangeRequest {

    @NotBlank(message = "El nuevo estado es obligatorio")
    private String newStatus;

    private String comments;

    private String reason;
}

