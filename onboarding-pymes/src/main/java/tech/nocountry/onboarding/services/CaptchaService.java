package tech.nocountry.onboarding.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.nocountry.onboarding.repositories.LoginAttemptRepository;

import java.time.LocalDateTime;

@Service
public class CaptchaService {

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Value("${security.captcha.required-after-attempts:3}")
    private int requiredAfterAttempts;

    @Value("${security.captcha.window-minutes:15}")
    private int windowMinutes;

    /**
     * Verificar si se requiere CAPTCHA para un email/IP
     * @param email Email del usuario (opcional)
     * @param ipAddress IP address del cliente
     * @return true si se requiere CAPTCHA
     */
    public boolean isCaptchaRequired(String email, String ipAddress) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(windowMinutes);
        
        // Contar intentos fallidos por IP
        long failedAttemptsByIp = loginAttemptRepository.countFailedAttemptsByIpSince(ipAddress, since);
        
        // Contar intentos fallidos por email si se proporciona
        long failedAttemptsByEmail = 0;
        if (email != null && !email.isEmpty()) {
            failedAttemptsByEmail = loginAttemptRepository.countFailedAttemptsByEmailSince(email, since);
        }
        
        // Requerir CAPTCHA si hay X o más intentos fallidos en la ventana de tiempo
        return failedAttemptsByIp >= requiredAfterAttempts || 
               (email != null && !email.isEmpty() && failedAttemptsByEmail >= requiredAfterAttempts);
    }

    /**
     * Verificar si se requiere CAPTCHA solo por IP
     */
    public boolean isCaptchaRequiredByIp(String ipAddress) {
        return isCaptchaRequired(null, ipAddress);
    }

    /**
     * Verificar si se requiere CAPTCHA solo por email
     */
    public boolean isCaptchaRequiredByEmail(String email) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(windowMinutes);
        long failedAttemptsByEmail = loginAttemptRepository.countFailedAttemptsByEmailSince(email, since);
        return failedAttemptsByEmail >= requiredAfterAttempts;
    }

    public int getRequiredAfterAttempts() {
        return requiredAfterAttempts;
    }

    public int getWindowMinutes() {
        return windowMinutes;
    }
}

