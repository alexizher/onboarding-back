package tech.nocountry.onboarding.modules.kyc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycVerificationResponse {
    private String verificationId;
    private String applicationId;
    private String userId;
    private String provider;
    private String verificationType;
    private String status;
    private Float score;
    private String result;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
}

