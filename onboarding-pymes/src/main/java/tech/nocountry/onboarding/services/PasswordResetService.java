package tech.nocountry.onboarding.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.nocountry.onboarding.entities.PasswordResetToken;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.repositories.PasswordResetTokenRepository;
import tech.nocountry.onboarding.repositories.UserRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PasswordResetService {

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityAuditService securityAuditService;

    // Duración del token en horas
    private static final int TOKEN_DURATION_HOURS = 1;
    private static final int MAX_ATTEMPTS_PER_HOUR = 3;

    /**
     * Generar token de recuperación de contraseña
     */
    public String generateResetToken(String email, String ipAddress, String userAgent) {
        // Verificar que el usuario existe
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            securityAuditService.logSecurityEvent(
                null, 
                "PASSWORD_RESET_ATTEMPT_INVALID_EMAIL", 
                ipAddress, 
                userAgent, 
                "Intento de recuperación de contraseña con email inexistente: " + email,
                "MEDIUM"
            );
            return null;
        }

        User user = userOptional.get();

        // Verificar límite de intentos
        if (hasExceededAttemptLimit(user.getUserId())) {
            securityAuditService.logSecurityEvent(
                user.getUserId(), 
                "PASSWORD_RESET_ATTEMPT_LIMIT_EXCEEDED", 
                ipAddress, 
                userAgent, 
                "Límite de intentos de recuperación de contraseña excedido",
                "HIGH"
            );
            return null;
        }

        // Invalidar tokens anteriores del usuario
        tokenRepository.invalidateAllUserTokens(user.getUserId());

        // Generar nuevo token
        String token = generateSecureToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(TOKEN_DURATION_HOURS);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .userId(user.getUserId())
                .expiresAt(expiresAt)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .used(false)
                .build();

        tokenRepository.save(resetToken);

        securityAuditService.logSecurityEvent(
            user.getUserId(), 
            "PASSWORD_RESET_TOKEN_GENERATED", 
            ipAddress, 
            userAgent, 
            "Token de recuperación de contraseña generado",
            "LOW"
        );

        return token;
    }

    /**
     * Validar token de recuperación
     */
    public boolean validateResetToken(String token) {
        Optional<PasswordResetToken> tokenOptional = tokenRepository.findByToken(token);
        
        if (tokenOptional.isEmpty()) {
            return false;
        }

        PasswordResetToken resetToken = tokenOptional.get();
        
        // Verificar si el token está usado o expirado
        if (resetToken.getUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }

        return true;
    }

    /**
     * Cambiar contraseña usando token
     */
    public boolean resetPassword(String token, String newPassword, String ipAddress, String userAgent) {
        Optional<PasswordResetToken> tokenOptional = tokenRepository.findByToken(token);
        
        if (tokenOptional.isEmpty() || !validateResetToken(token)) {
            securityAuditService.logSecurityEvent(
                null, 
                "PASSWORD_RESET_INVALID_TOKEN", 
                ipAddress, 
                userAgent, 
                "Intento de cambio de contraseña con token inválido",
                "MEDIUM"
            );
            return false;
        }

        PasswordResetToken resetToken = tokenOptional.get();
        Optional<User> userOptional = userRepository.findById(resetToken.getUserId());
        
        if (userOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();
        
        // Cambiar contraseña
        user.setPasswordHash(newPassword); // El hash se aplicará en el AuthService
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Marcar token como usado
        tokenRepository.markTokenAsUsed(token);

        securityAuditService.logSecurityEvent(
            user.getUserId(), 
            "PASSWORD_RESET_SUCCESS", 
            ipAddress, 
            userAgent, 
            "Contraseña cambiada exitosamente mediante token de recuperación",
            "LOW"
        );

        return true;
    }

    /**
     * Verificar si se ha excedido el límite de intentos
     */
    private boolean hasExceededAttemptLimit(String userId) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<PasswordResetToken> recentTokens = tokenRepository.findValidTokensByUserId(userId, oneHourAgo);
        return recentTokens.size() >= MAX_ATTEMPTS_PER_HOUR;
    }

    /**
     * Generar token seguro
     */
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Limpiar tokens expirados (ejecutar periódicamente)
     */
    @Scheduled(fixedRate = 3600000) // Cada hora
    public void cleanupExpiredTokens() {
        List<PasswordResetToken> expiredTokens = tokenRepository.findExpiredTokens(LocalDateTime.now());
        tokenRepository.deleteAll(expiredTokens);
    }
}
