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
    
    // Seguridad del token
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final int COOLDOWN_MINUTES = 15;

    /**
     * Generar token de recuperación de contraseña
     * Nota: No revela si el email existe o no por seguridad
     */
    public String generateResetToken(String email, String ipAddress, String userAgent) {
        // Verificar que el usuario existe
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            // Log de seguridad pero NO revelar que el email no existe
            securityAuditService.logSecurityEvent(
                null, 
                "PASSWORD_RESET_ATTEMPT_INVALID_EMAIL", 
                ipAddress, 
                userAgent, 
                "Intento de recuperación de contraseña con email inexistente",
                "MEDIUM"
            );
            // No devolver null directamente, devolver un mensaje genérico
            // Retornar null pero el servicio llamador debe manejar el mensaje genérico
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

        // Nota: La notificación de password reset se manejará desde AuthService
        // donde se llama a generatePasswordResetToken, para evitar dependencia circular

        return token;
    }

    /**
     * Validar token de recuperación con medidas de seguridad
     */
    public boolean validateResetToken(String token, String ipAddress, String userAgent) {
        Optional<PasswordResetToken> tokenOptional = tokenRepository.findByToken(token);
        
        if (tokenOptional.isEmpty()) {
            // Registrar intento con token inexistente
            securityAuditService.logSecurityEvent(
                null,
                "PASSWORD_RESET_INVALID_TOKEN_ATTEMPT",
                ipAddress,
                userAgent,
                "Intento de validación con token inexistente",
                "MEDIUM"
            );
            return false;
        }

        PasswordResetToken resetToken = tokenOptional.get();
        
        // Incrementar contador de validaciones
        resetToken.setValidationCount(resetToken.getValidationCount() + 1);
        resetToken.setLastAttemptAt(LocalDateTime.now());
        tokenRepository.save(resetToken);
        
        // Verificar si el token está bloqueado
        if (resetToken.getIsBlocked()) {
            securityAuditService.logSecurityEvent(
                resetToken.getUserId(),
                "PASSWORD_RESET_BLOCKED_TOKEN_ATTEMPT",
                ipAddress,
                userAgent,
                "Intento de usar token bloqueado. Razón: " + resetToken.getBlockedReason(),
                "HIGH"
            );
            return false;
        }
        
        // Verificar cooldown
        if (resetToken.getCooldownUntil() != null && 
            resetToken.getCooldownUntil().isAfter(LocalDateTime.now())) {
            securityAuditService.logSecurityEvent(
                resetToken.getUserId(),
                "PASSWORD_RESET_COOLDOWN_ACTIVE",
                ipAddress,
                userAgent,
                "Intento durante período de cooldown",
                "MEDIUM"
            );
            return false;
        }
        
        // Verificar si el token está usado
        if (resetToken.getUsed()) {
            securityAuditService.logSecurityEvent(
                resetToken.getUserId(),
                "PASSWORD_RESET_USED_TOKEN_ATTEMPT",
                ipAddress,
                userAgent,
                "Intento de usar token ya utilizado",
                "HIGH"
            );
            return false;
        }
        
        // Verificar si el token está expirado
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            securityAuditService.logSecurityEvent(
                resetToken.getUserId(),
                "PASSWORD_RESET_EXPIRED_TOKEN_ATTEMPT",
                ipAddress,
                userAgent,
                "Intento de usar token expirado",
                "MEDIUM"
            );
            return false;
        }
        
        // Validar IP y User Agent (warning si es diferente, pero no bloquear)
        boolean ipMismatch = resetToken.getIpAddress() != null && 
                            !resetToken.getIpAddress().equals(ipAddress);
        boolean userAgentMismatch = resetToken.getUserAgent() != null && 
                                   !resetToken.getUserAgent().equals(userAgent);
        
        if (ipMismatch || userAgentMismatch) {
            String details = "Validación con origen diferente. ";
            if (ipMismatch) details += "IP: " + resetToken.getIpAddress() + " vs " + ipAddress + ". ";
            if (userAgentMismatch) details += "User Agent diferente.";
            
            securityAuditService.logSecurityEvent(
                resetToken.getUserId(),
                "PASSWORD_RESET_SUSPICIOUS_ORIGIN",
                ipAddress,
                userAgent,
                details,
                "MEDIUM"
            );
            // No bloquear, pero registrar como sospechoso
        }

        return true;
    }
    
    /**
     * Validar token (versión simple para compatibilidad)
     */
    public boolean validateResetToken(String token) {
        return validateResetToken(token, null, null);
    }

    /**
     * Cambiar contraseña usando token con medidas de seguridad mejoradas
     */
    public boolean resetPassword(String token, String newPassword, String ipAddress, String userAgent) {
        Optional<PasswordResetToken> tokenOptional = tokenRepository.findByToken(token);
        
        if (tokenOptional.isEmpty()) {
            securityAuditService.logSecurityEvent(
                null, 
                "PASSWORD_RESET_INVALID_TOKEN", 
                ipAddress, 
                userAgent, 
                "Intento de cambio de contraseña con token inexistente",
                "HIGH"
            );
            return false;
        }

        PasswordResetToken resetToken = tokenOptional.get();
        
        // Validar token con medidas de seguridad
        if (!validateResetToken(token, ipAddress, userAgent)) {
            // Incrementar intentos fallidos
            resetToken.setFailedAttempts(resetToken.getFailedAttempts() + 1);
            resetToken.setLastAttemptAt(LocalDateTime.now());
            
            // Bloquear si excede máximo de intentos
            if (resetToken.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                resetToken.setIsBlocked(true);
                resetToken.setBlockedAt(LocalDateTime.now());
                resetToken.setBlockedReason("MAX_FAILED_ATTEMPTS");
                resetToken.setCooldownUntil(LocalDateTime.now().plusMinutes(COOLDOWN_MINUTES));
                
                securityAuditService.logSecurityEvent(
                    resetToken.getUserId(),
                    "PASSWORD_RESET_TOKEN_BLOCKED",
                    ipAddress,
                    userAgent,
                    "Token bloqueado por exceso de intentos fallidos: " + resetToken.getFailedAttempts(),
                    "CRITICAL"
                );
                
                // Invalidar todos los tokens del usuario por seguridad
                tokenRepository.invalidateAllUserTokens(resetToken.getUserId());
            }
            
            tokenRepository.save(resetToken);
            return false;
        }

        Optional<User> userOptional = userRepository.findById(resetToken.getUserId());
        
        if (userOptional.isEmpty()) {
            securityAuditService.logSecurityEvent(
                resetToken.getUserId(),
                "PASSWORD_RESET_USER_NOT_FOUND",
                ipAddress,
                userAgent,
                "Usuario asociado al token no encontrado",
                "HIGH"
            );
            return false;
        }

        User user = userOptional.get();
        
        // Verificar que la nueva contraseña no esté en el historial (si PasswordHistoryService está disponible)
        // Nota: Esta verificación se puede hacer aquí o en AuthService
        // Por ahora, solo cambiamos la contraseña
        
        // Cambiar contraseña
        user.setPasswordHash(newPassword); // El hash ya viene aplicado desde AuthService
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        // Nota: La gestión del historial de contraseñas se hace en AuthService
        // cuando se llama a resetPasswordWithToken desde ahí

        // Marcar token como usado y guardar información de uso
        resetToken.setUsed(true);
        resetToken.setUsedAt(LocalDateTime.now());
        resetToken.setUsedFromIp(ipAddress);
        resetToken.setUsedFromUserAgent(userAgent);
        tokenRepository.save(resetToken);

        // Invalidar todos los demás tokens del usuario
        tokenRepository.invalidateAllUserTokens(resetToken.getUserId());

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
