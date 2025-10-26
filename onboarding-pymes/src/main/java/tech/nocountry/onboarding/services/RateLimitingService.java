package tech.nocountry.onboarding.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.nocountry.onboarding.entities.LoginAttempt;
import tech.nocountry.onboarding.repositories.LoginAttemptRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class RateLimitingService {

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Autowired
    private SecurityAuditService securityAuditService;

    // Límites de intentos
    private static final int MAX_ATTEMPTS_PER_IP = 5;
    private static final int MAX_ATTEMPTS_PER_EMAIL = 3;
    private static final int LOCKOUT_DURATION_MINUTES = 15;

    /**
     * Registrar intento de login
     */
    public void recordLoginAttempt(String ipAddress, String email, boolean successful, String failureReason, String userAgent) {
        LoginAttempt attempt = LoginAttempt.builder()
                .attemptId(java.util.UUID.randomUUID().toString())
                .ipAddress(ipAddress)
                .email(email)
                .successful(successful)
                .failureReason(failureReason)
                .userAgent(userAgent)
                .attemptTime(LocalDateTime.now())
                .build();

        loginAttemptRepository.save(attempt);

        // Log de seguridad
        String eventType = successful ? "LOGIN_SUCCESS" : "LOGIN_FAILED";
        String severity = successful ? "LOW" : "MEDIUM";
        securityAuditService.logSecurityEvent(
            null, 
            eventType, 
            ipAddress, 
            userAgent, 
            successful ? "Login exitoso" : "Login fallido: " + failureReason,
            severity
        );
    }

    /**
     * Verificar si una IP está bloqueada
     */
    public boolean isIpBlocked(String ipAddress) {
        LocalDateTime lockoutTime = LocalDateTime.now().minusMinutes(LOCKOUT_DURATION_MINUTES);
        long recentAttempts = loginAttemptRepository.countFailedAttemptsByIpSince(ipAddress, lockoutTime);
        
        if (recentAttempts >= MAX_ATTEMPTS_PER_IP) {
            securityAuditService.logSecurityEvent(
                null, 
                "IP_BLOCKED", 
                ipAddress, 
                null, 
                "IP bloqueada por exceso de intentos de login",
                "HIGH"
            );
            return true;
        }
        
        return false;
    }

    /**
     * Verificar si un email está bloqueado
     */
    public boolean isEmailBlocked(String email) {
        LocalDateTime lockoutTime = LocalDateTime.now().minusMinutes(LOCKOUT_DURATION_MINUTES);
        long recentAttempts = loginAttemptRepository.countFailedAttemptsByEmailSince(email, lockoutTime);
        
        if (recentAttempts >= MAX_ATTEMPTS_PER_EMAIL) {
            securityAuditService.logSecurityEvent(
                null, 
                "EMAIL_BLOCKED", 
                null, 
                null, 
                "Email bloqueado por exceso de intentos: " + email,
                "HIGH"
            );
            return true;
        }
        
        return false;
    }

    /**
     * Verificar si se puede hacer un intento de login
     */
    public boolean canAttemptLogin(String ipAddress, String email) {
        return !isIpBlocked(ipAddress) && !isEmailBlocked(email);
    }

    /**
     * Obtener intentos recientes de una IP
     */
    public List<LoginAttempt> getRecentAttemptsByIp(String ipAddress) {
        return loginAttemptRepository.findTop10ByIpAddressOrderByAttemptTimeDesc(ipAddress);
    }

    /**
     * Obtener intentos recientes de un email
     */
    public List<LoginAttempt> getRecentAttemptsByEmail(String email) {
        return loginAttemptRepository.findTop10ByEmailOrderByAttemptTimeDesc(email);
    }

    /**
     * Limpiar intentos antiguos (ejecutar periódicamente)
     */
    @Scheduled(fixedRate = 3600000) // Cada hora
    public void cleanupOldAttempts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7); // Mantener por 7 días
        loginAttemptRepository.deleteOldAttempts(cutoff);
    }

    /**
     * Obtener estadísticas de intentos
     */
    public String getAttemptStats(String ipAddress) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long failedAttempts = loginAttemptRepository.countFailedAttemptsByIpSince(ipAddress, oneHourAgo);
        long totalAttempts = loginAttemptRepository.findTop10ByIpAddressOrderByAttemptTimeDesc(ipAddress).size();
        
        return String.format("IP: %s - Intentos fallidos (1h): %d, Total recientes: %d", 
                           ipAddress, failedAttempts, totalAttempts);
    }
}
