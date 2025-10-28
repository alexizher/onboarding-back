package tech.nocountry.onboarding.modules.applications.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusHistoryResponse {

    private String historyId;
    private String applicationId;
    private String previousStatus;
    private String newStatus;
    private String comments;
    private String changedByRole;
    private String changedBy;
    private LocalDateTime changedAt;
}

