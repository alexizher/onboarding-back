package tech.nocountry.onboarding.modules.kyc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.modules.kyc.dto.KycVerificationResult;
import tech.nocountry.onboarding.modules.kyc.provider.KycProvider;
import tech.nocountry.onboarding.repositories.UserRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class IdentityVerificationService {

    private final UserRepository userRepository;
    private final KycProvider kycProvider; // Se inyectará MockKycProvider por defecto

    /**
     * Verifica la identidad de un usuario
     */
    public KycVerificationResult verifyUserIdentity(String userId) {
        log.info("Verifying identity for user: {}", userId);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Validar que tenga los datos necesarios
        if (user.getDni() == null || user.getDni().isBlank()) {
            throw new RuntimeException("El usuario no tiene DNI registrado");
        }

        if (user.getFullName() == null || user.getFullName().isBlank()) {
            throw new RuntimeException("El usuario no tiene nombre completo registrado");
        }

        // Llamar al proveedor KYC
        return kycProvider.verifyIdentity(userId, user.getDni(), user.getFullName());
    }

    /**
     * Verifica documentos adicionales
     */
    public KycVerificationResult verifyDocuments(String userId, Object documentData) {
        log.info("Verifying documents for user: {}", userId);

        userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return kycProvider.verifyDocument(userId, documentData);
    }

    /**
     * Realiza verificación completa
     */
    public KycVerificationResult performFullVerification(String userId, Object verificationData) {
        log.info("Performing full verification for user: {}", userId);

        userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return kycProvider.performFullVerification(userId, verificationData);
    }
}

