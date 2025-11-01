package tech.nocountry.onboarding.modules.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessmentResponse {
    private String assessmentId;
    private String applicationId;
    private Float score;
    private String level;
    private String details;
    private String assessedBy;
    private LocalDateTime assessedAt;
    private Boolean isAutomated;
    private String recommendation;
}

