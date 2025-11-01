package tech.nocountry.onboarding.modules.applications.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationAssignmentRequest {
    private String applicationId;
    private String assignedToUserId;  // ID del analista al que se asigna
    private String comments;  // Comentarios opcionales sobre la asignación
}

