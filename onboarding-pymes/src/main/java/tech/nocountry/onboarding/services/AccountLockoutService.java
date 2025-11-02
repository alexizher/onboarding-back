package tech.nocountry.onboarding.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.repositories.UserRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@Transactional
public class AccountLockoutService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityAuditService securityAuditService;

    // Configuración de bloqueo progresivo
    private static final int MAX_ATTEMPTS_BEFORE_LOCKOUT = 3;
    private static final int INITIAL_LOCKOUT_HOURS = 2; // 2 horas para el primer bloqueo
    
    /**
     * Calcula la duración del bloqueo según el nivel actual
     * Nivel 1: 2 horas
     * Nivel 2: 4 horas
     * Nivel 3: 8 horas
     * Nivel 4: 16 horas
     * Nivel 5+: 24 horas (máximo)
     */
    private int calculateLockoutHours(int lockoutLevel) {
        if (lockoutLevel <= 1) {
            return INITIAL_LOCKOUT_HOURS; // 2 horas
        } else if (lockoutLevel <= 5) {
            // Exponencial: 2^level horas (máximo 32 horas)
            return (int) Math.pow(2, lockoutLevel);
        } else {
            // Máximo 24 horas para niveles superiores
            return 24;
        }
    }

    /**
     * Verifica si la cuenta está bloqueada
     */
    public boolean isAccountLocked(User user) {
        if (user.getLockoutUntil() == null) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(user.getLockoutUntil())) {
            return true; // Aún está bloqueado
        }
        
        // El bloqueo expiró, pero no resetear automáticamente
        // Se reseteará cuando haya un login exitoso
        return false;
    }

    /**
     * Obtiene el tiempo restante de bloqueo en minutos
     */
    public long getRemainingLockoutMinutes(User user) {
        if (user.getLockoutUntil() == null) {
            return 0;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(user.getLockoutUntil())) {
            return ChronoUnit.MINUTES.between(now, user.getLockoutUntil());
        }
        
        return 0;
    }

    /**
     * Incrementa el contador de intentos fallidos
     * Si llega al límite, bloquea la cuenta progresivamente
     */
    public void recordFailedAttempt(User user, String ipAddress, String userAgent) {
        int currentAttempts = user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0;
        int newAttempts = currentAttempts + 1;
        
        // Si intentan login mientras están bloqueados, incrementar nivel
        if (isAccountLocked(user)) {
            int currentLevel = user.getLockoutLevel() != null ? user.getLockoutLevel() : 1;
            int newLevel = currentLevel + 1;
            int lockoutHours = calculateLockoutHours(newLevel);
            LocalDateTime newLockoutUntil = LocalDateTime.now().plusHours(lockoutHours);
            
            user.setLockoutLevel(newLevel);
            user.setLockoutUntil(newLockoutUntil);
            user.setFailedLoginAttempts(0); // Resetear contador al aplicar nuevo bloqueo
            
            securityAuditService.logSecurityEvent(
                user.getUserId(),
                "ACCOUNT_LOCKOUT_ESCALATED",
                ipAddress,
                userAgent,
                String.format("Intento de login durante bloqueo. Nuevo bloqueo: %d horas (nivel %d)", 
                    lockoutHours, newLevel),
                "HIGH"
            );
            
            userRepository.save(user);
            return;
        }
        
        // Incrementar contador de intentos
        user.setFailedLoginAttempts(newAttempts);
        
        // Si alcanza el límite, aplicar bloqueo inicial
        if (newAttempts >= MAX_ATTEMPTS_BEFORE_LOCKOUT) {
            int lockoutLevel = 1;
            int lockoutHours = calculateLockoutHours(lockoutLevel);
            LocalDateTime lockoutUntil = LocalDateTime.now().plusHours(lockoutHours);
            
            user.setLockoutLevel(lockoutLevel);
            user.setLockoutUntil(lockoutUntil);
            user.setFailedLoginAttempts(0); // Resetear contador al aplicar bloqueo
            
            securityAuditService.logSecurityEvent(
                user.getUserId(),
                "ACCOUNT_LOCKED",
                ipAddress,
                userAgent,
                String.format("Cuenta bloqueada por %d intentos fallidos. Bloqueo: %d horas (nivel %d)", 
                    MAX_ATTEMPTS_BEFORE_LOCKOUT, lockoutHours, lockoutLevel),
                "HIGH"
            );
        }
        
        userRepository.save(user);
    }

    /**
     * Resetea el contador de intentos fallidos y el bloqueo
     * Se llama cuando el login es exitoso
     */
    public void resetLockout(User user) {
        if (user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
        }
        if (user.getLockoutUntil() != null) {
            user.setLockoutUntil(null);
        }
        if (user.getLockoutLevel() != null && user.getLockoutLevel() > 0) {
            user.setLockoutLevel(0);
        }
        userRepository.save(user);
    }

    /**
     * Desbloquea manualmente la cuenta (solo ADMIN)
     */
    public void unlockAccount(User user, String unlockedBy, String reason) {
        user.setLockoutUntil(null);
        user.setLockoutLevel(0);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
        
        securityAuditService.logSecurityEvent(
            user.getUserId(),
            "ACCOUNT_UNLOCKED",
            null,
            null,
            String.format("Cuenta desbloqueada manualmente por %s. Razón: %s", unlockedBy, reason),
            "MEDIUM"
        );
    }

    /**
     * Obtiene el mensaje de bloqueo para mostrar al usuario
     */
    public String getLockoutMessage(User user) {
        if (!isAccountLocked(user)) {
            return null;
        }
        
        long remainingMinutes = getRemainingLockoutMinutes(user);
        int lockoutLevel = user.getLockoutLevel() != null ? user.getLockoutLevel() : 1;
        
        if (remainingMinutes < 60) {
            return String.format("Cuenta bloqueada por seguridad. Intenta nuevamente en %d minutos.", remainingMinutes);
        } else {
            long hours = remainingMinutes / 60;
            long minutes = remainingMinutes % 60;
            if (minutes == 0) {
                return String.format("Cuenta bloqueada por seguridad (nivel %d). Intenta nuevamente en %d horas.", 
                    lockoutLevel, hours);
            } else {
                return String.format("Cuenta bloqueada por seguridad (nivel %d). Intenta nuevamente en %d horas y %d minutos.", 
                    lockoutLevel, hours, minutes);
            }
        }
    }
}

