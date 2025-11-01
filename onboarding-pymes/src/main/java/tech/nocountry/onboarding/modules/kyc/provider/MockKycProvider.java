package tech.nocountry.onboarding.modules.kyc.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.nocountry.onboarding.modules.kyc.dto.KycVerificationResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Component
@Slf4j
public class MockKycProvider implements KycProvider {

    private final ObjectMapper objectMapper;
    private final Random random;

    public MockKycProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.random = new Random();
    }

    @Override
    public String getProviderName() {
        return "Mock";
    }

    @Override
    public KycVerificationResult verifyIdentity(String userId, String dni, String fullName) {
        log.info("Mock KYC: Verificando identidad para usuario {} con DNI {}", userId, dni);

        // Simular proceso de verificación (delay simulado)
        try {
            Thread.sleep(500); // Simular latencia
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Validaciones básicas mock
        boolean isValid = isValidIdentity(dni, fullName);
        Float score = isValid ? 85.0f + random.nextFloat() * 15.0f : 20.0f + random.nextFloat() * 30.0f;
        String status = isValid ? "verified" : "rejected";

        Map<String, Object> details = new HashMap<>();
        details.put("dni", dni);
        details.put("fullName", fullName);
        details.put("provider", "Mock");
        details.put("verificationType", "IDENTITY");
        details.put("timestamp", java.time.Instant.now().toString());

        String providerResponse;
        try {
            providerResponse = objectMapper.writeValueAsString(details);
        } catch (Exception e) {
            providerResponse = "{}";
        }

        return KycVerificationResult.builder()
                .status(status)
                .score(score)
                .details(details)
                .providerResponse(providerResponse)
                .reason(isValid ? null : "Identidad no verificada - datos inconsistentes")
                .verificationId("MOCK-" + System.currentTimeMillis())
                .build();
    }

    @Override
    public KycVerificationResult verifyDocument(String userId, Object documentData) {
        log.info("Mock KYC: Verificando documento para usuario {}", userId);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simular verificación de documento
        boolean isValid = random.nextFloat() > 0.2f; // 80% de éxito
        Float score = isValid ? 80.0f + random.nextFloat() * 20.0f : 30.0f + random.nextFloat() * 30.0f;
        String status = isValid ? "verified" : "rejected";

        Map<String, Object> details = new HashMap<>();
        details.put("userId", userId);
        details.put("provider", "Mock");
        details.put("verificationType", "DOCUMENT");
        details.put("timestamp", java.time.Instant.now().toString());

        String providerResponse;
        try {
            providerResponse = objectMapper.writeValueAsString(details);
        } catch (Exception e) {
            providerResponse = "{}";
        }

        return KycVerificationResult.builder()
                .status(status)
                .score(score)
                .details(details)
                .providerResponse(providerResponse)
                .reason(isValid ? null : "Documento no válido o no legible")
                .verificationId("MOCK-DOC-" + System.currentTimeMillis())
                .build();
    }

    @Override
    public KycVerificationResult performFullVerification(String userId, Object verificationData) {
        log.info("Mock KYC: Verificación completa para usuario {}", userId);

        try {
            Thread.sleep(1000); // Simular proceso más largo
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simular verificación completa
        boolean isValid = random.nextFloat() > 0.15f; // 85% de éxito
        Float score = isValid ? 90.0f + random.nextFloat() * 10.0f : 25.0f + random.nextFloat() * 25.0f;
        String status = isValid ? "verified" : "rejected";

        Map<String, Object> details = new HashMap<>();
        details.put("userId", userId);
        details.put("provider", "Mock");
        details.put("verificationType", "FULL");
        details.put("identityVerified", isValid);
        details.put("documentVerified", isValid);
        details.put("biometricVerified", isValid ? random.nextFloat() > 0.3f : false);
        details.put("timestamp", java.time.Instant.now().toString());

        String providerResponse;
        try {
            providerResponse = objectMapper.writeValueAsString(details);
        } catch (Exception e) {
            providerResponse = "{}";
        }

        return KycVerificationResult.builder()
                .status(status)
                .score(score)
                .details(details)
                .providerResponse(providerResponse)
                .reason(isValid ? null : "Verificación completa fallida - múltiples inconsistencias")
                .verificationId("MOCK-FULL-" + System.currentTimeMillis())
                .build();
    }

    /**
     * Valida formato básico de identidad (mock)
     */
    private boolean isValidIdentity(String dni, String fullName) {
        if (dni == null || dni.isBlank() || fullName == null || fullName.isBlank()) {
            return false;
        }
        
        // Validar formato DNI (ejemplo: debe tener al menos 6 dígitos)
        String dniClean = dni.replaceAll("[^0-9]", "");
        if (dniClean.length() < 6) {
            return false;
        }

        // Validar que el nombre tenga al menos 2 palabras
        String[] nameParts = fullName.trim().split("\\s+");
        if (nameParts.length < 2) {
            return false;
        }

        return true;
    }
}

