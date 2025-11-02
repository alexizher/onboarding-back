package tech.nocountry.onboarding.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.nocountry.onboarding.entities.EmailVerificationToken;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.repositories.EmailVerificationTokenRepository;
import tech.nocountry.onboarding.repositories.UserRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class EmailVerificationService {

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityAuditService securityAuditService;

    @Autowired
    private tech.nocountry.onboarding.services.NotificationService notificationService;

    @Value("${email.verification.token-expiration-minutes:60}")
    private int tokenExpirationMinutes;

    /**
     * Generar token de verificación de email
     */
    public EmailVerificationToken generateVerificationToken(String userId, String email, String ipAddress, String userAgent) {
        // Invalidar tokens anteriores
        tokenRepository.invalidateAllUserTokens(userId, LocalDateTime.now());

        // Generar token seguro (32 bytes)
        SecureRandom random = new SecureRandom();
        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(tokenExpirationMinutes);

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .tokenId(UUID.randomUUID().toString())
                .userId(userId)
                .token(token) // Guardar token original para enviar por email
                .email(email)
                .expiresAt(expiresAt)
                .isUsed(false)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        return tokenRepository.save(verificationToken);
    }

    /**
     * Verificar token de email
     */
    public boolean verifyEmail(String token, String ipAddress, String userAgent) {
        LocalDateTime now = LocalDateTime.now();

        Optional<EmailVerificationToken> tokenOptional = tokenRepository.findValidToken(token, now);

        if (tokenOptional.isEmpty()) {
            return false;
        }

        EmailVerificationToken verificationToken = tokenOptional.get();

        // Marcar token como usado
        verificationToken.setIsUsed(true);
        verificationToken.setVerifiedAt(now);
        tokenRepository.save(verificationToken);

        // Actualizar usuario
        Optional<User> userOptional = userRepository.findById(verificationToken.getUserId());
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setEmailVerified(true);
            user.setEmailVerifiedAt(now);
            userRepository.save(user);

            // Log de seguridad
            securityAuditService.logSecurityEvent(
                user.getUserId(),
                "EMAIL_VERIFIED",
                ipAddress,
                userAgent,
                "Email verificado exitosamente: " + verificationToken.getEmail(),
                "LOW"
            );

            return true;
        }

        return false;
    }

    /**
     * Reenviar token de verificación
     */
    public EmailVerificationToken resendVerificationToken(String userId, String ipAddress, String userAgent) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("Usuario no encontrado");
        }

        User user = userOptional.get();
        EmailVerificationToken token = generateVerificationToken(userId, user.getEmail(), ipAddress, userAgent);
        
        // Enviar notificación con el token
        if (token != null) {
            try {
                notificationService.notifyEmailVerification(userId, token.getToken());
            } catch (Exception e) {
                // No fallar si hay error al enviar notificación
                System.out.println("Error al enviar notificación de verificación: " + e.getMessage());
            }
        }
        
        return token;
    }

    /**
     * Verificar si un email está verificado
     */
    public boolean isEmailVerified(String userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();
        return user.getEmailVerified() != null && user.getEmailVerified();
    }


    /**
     * Limpiar tokens expirados (ejecutar periódicamente)
     */
    @Scheduled(fixedRate = 3600000) // Cada hora
    public void cleanupExpiredTokens() {
        // Los tokens expirados no se eliminan automáticamente, se marcan como usados o se mantienen para auditoría
        // Esta limpieza es opcional
    }
}

