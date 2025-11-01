package tech.nocountry.onboarding.modules.kyc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.nocountry.onboarding.entities.CreditApplication;
import tech.nocountry.onboarding.entities.KycVerification;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.modules.kyc.dto.*;
import tech.nocountry.onboarding.modules.kyc.provider.KycProvider;
import tech.nocountry.onboarding.modules.kyc.provider.MockKycProvider;
import tech.nocountry.onboarding.repositories.CreditApplicationRepository;
import tech.nocountry.onboarding.repositories.KycVerificationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class KycService {

    private final KycVerificationRepository kycVerificationRepository;
    private final CreditApplicationRepository applicationRepository;
    private final IdentityVerificationService identityVerificationService;
    private final MockKycProvider mockKycProvider; // Proveedor por defecto
    private final ObjectMapper objectMapper;

    /**
     * Inicia una verificación KYC para una solicitud
     */
    @Transactional
    public KycVerificationResponse initiateVerification(String applicationId, String verificationType, String provider) {
        log.info("Initiating KYC verification for application: {}, type: {}, provider: {}", 
                 applicationId, verificationType, provider);

        CreditApplication application = applicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        User user = application.getUser();
        if (user == null) {
            throw new RuntimeException("Usuario de la solicitud no encontrado");
        }

        // Seleccionar proveedor (por defecto Mock)
        KycProvider selectedProvider = provider != null && !provider.isBlank() 
                ? getProviderByName(provider) 
                : mockKycProvider;

        // Realizar verificación según el tipo
        KycVerificationResult result;
        try {
            switch (verificationType.toUpperCase()) {
                case "IDENTITY":
                    result = identityVerificationService.verifyUserIdentity(user.getUserId());
                    break;
                case "DOCUMENT":
                    result = identityVerificationService.verifyDocuments(user.getUserId(), null);
                    break;
                case "FULL":
                    result = identityVerificationService.performFullVerification(user.getUserId(), null);
                    break;
                default:
                    throw new RuntimeException("Tipo de verificación no válido: " + verificationType);
            }
        } catch (Exception e) {
            log.error("Error performing KYC verification: {}", e.getMessage(), e);
            result = KycVerificationResult.builder()
                    .status("failed")
                    .score(0.0f)
                    .reason("Error en la verificación: " + e.getMessage())
                    .build();
        }

        // Guardar verificación
        KycVerification verification = KycVerification.builder()
                .application(application)
                .user(user)
                .provider(selectedProvider.getProviderName())
                .verificationType(verificationType)
                .status(result.getStatus())
                .score(result.getScore())
                .result(convertResultToJson(result))
                .verifiedAt("verified".equals(result.getStatus()) ? LocalDateTime.now() : null)
                .createdAt(LocalDateTime.now())
                .build();

        KycVerification saved = kycVerificationRepository.save(verification);
        log.info("KYC verification saved: {} (status: {}, score: {})", 
                 saved.getVerificationId(), saved.getStatus(), saved.getScore());

        return mapToResponse(saved);
    }

    /**
     * Obtiene todas las verificaciones de una solicitud
     */
    @Transactional(readOnly = true)
    public List<KycVerificationResponse> getVerificationsByApplication(String applicationId) {
        log.info("Getting KYC verifications for application: {}", applicationId);

        List<KycVerification> verifications = kycVerificationRepository.findByApplicationId(applicationId);
        return verifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene todas las verificaciones de un usuario
     */
    @Transactional(readOnly = true)
    public List<KycVerificationResponse> getVerificationsByUser(String userId) {
        log.info("Getting KYC verifications for user: {}", userId);

        List<KycVerification> verifications = kycVerificationRepository.findByUserId(userId);
        return verifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene la verificación más reciente de un tipo específico para una solicitud
     */
    @Transactional(readOnly = true)
    public KycVerificationResponse getLatestVerification(String applicationId, String verificationType) {
        log.info("Getting latest KYC verification for application: {}, type: {}", applicationId, verificationType);

        KycVerification verification = kycVerificationRepository
                .findLatestByApplicationIdAndVerificationType(applicationId, verificationType)
                .orElseThrow(() -> new RuntimeException("No se encontró verificación del tipo " + verificationType));

        return mapToResponse(verification);
    }

    /**
     * Obtiene estadísticas de verificaciones KYC
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getKycStatistics() {
        log.info("Getting KYC statistics");

        java.util.Map<String, Object> stats = new java.util.HashMap<>();

        long total = kycVerificationRepository.count();
        long pending = kycVerificationRepository.countByStatus("pending");
        long verified = kycVerificationRepository.countByStatus("verified");
        long rejected = kycVerificationRepository.countByStatus("rejected");
        long failed = kycVerificationRepository.countByStatus("failed");

        long mockCount = kycVerificationRepository.countByProvider("Mock");

        stats.put("total", total);
        stats.put("pending", pending);
        stats.put("verified", verified);
        stats.put("rejected", rejected);
        stats.put("failed", failed);
        stats.put("mockProviderCount", mockCount);

        if (total > 0) {
            stats.put("verifiedPercentage", (double) verified / total * 100);
            stats.put("rejectedPercentage", (double) rejected / total * 100);
            stats.put("successRate", (double) (verified + rejected) / total * 100);
        }

        return stats;
    }

    private KycVerificationResponse mapToResponse(KycVerification verification) {
        String applicationId = null;
        try {
            if (verification.getApplication() != null) {
                applicationId = verification.getApplication().getApplicationId();
            }
        } catch (Exception e) {
            log.warn("Error accessing application: {}", e.getMessage());
        }

        String userId = null;
        try {
            if (verification.getUser() != null) {
                userId = verification.getUser().getUserId();
            }
        } catch (Exception e) {
            log.warn("Error accessing user: {}", e.getMessage());
        }

        return KycVerificationResponse.builder()
                .verificationId(verification.getVerificationId())
                .applicationId(applicationId)
                .userId(userId)
                .provider(verification.getProvider())
                .verificationType(verification.getVerificationType())
                .status(verification.getStatus())
                .score(verification.getScore())
                .result(verification.getResult())
                .verifiedAt(verification.getVerifiedAt())
                .createdAt(verification.getCreatedAt())
                .build();
    }

    private String convertResultToJson(KycVerificationResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.warn("Error converting result to JSON: {}", e.getMessage());
            return "{}";
        }
    }

    private KycProvider getProviderByName(String providerName) {
        // Por ahora solo Mock, pero extensible para otros proveedores
        if ("Mock".equalsIgnoreCase(providerName) || providerName == null || providerName.isBlank()) {
            return mockKycProvider;
        }
        // En el futuro: return dataCreditoProvider, etc.
        log.warn("Provider {} not found, using Mock", providerName);
        return mockKycProvider;
    }
}

