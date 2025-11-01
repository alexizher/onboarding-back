package tech.nocountry.onboarding.modules.kyc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycVerificationRequest {
    private String applicationId;
    private String verificationType; // IDENTITY, DOCUMENT, BIOMETRIC, FULL
    private String provider; // Mock, DataCrédito, etc. (opcional, usa por defecto)
    private Object verificationData; // Datos adicionales para la verificación
}

