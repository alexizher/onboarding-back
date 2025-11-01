package tech.nocountry.onboarding.modules.kyc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycVerificationResult {
    private String status; // verified, rejected, failed, pending
    private Float score; // Score de confianza (0-100)
    private Map<String, Object> details; // Detalles de la verificación
    private String providerResponse; // Respuesta del proveedor
    private String reason; // Razón si fue rechazada
    private String verificationId; // ID de la verificación en el proveedor
}

