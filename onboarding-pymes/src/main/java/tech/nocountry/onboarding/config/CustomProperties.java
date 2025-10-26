package tech.nocountry.onboarding.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties
public class CustomProperties {
    
    // JWT Properties
    private String jwtSecret = "mySecretKey123456789012345678901234567890";
    private long jwtExpiration = 86400000;
    
    // Password Properties
    private int passwordMinLength = 8;
    private int passwordMaxLength = 128;
    private boolean passwordRequireUppercase = true;
    private boolean passwordRequireLowercase = true;
    private boolean passwordRequireDigits = true;
    private boolean passwordRequireSpecialChars = false;
    
    // Rate Limiting Properties
    private int rateLimitLoginAttemptsPerIp = 5;
    private int rateLimitLoginAttemptsPerEmail = 3;
    private int rateLimitLockoutDurationMinutes = 15;
    
    // Session Properties
    private int sessionMaxConcurrent = 3;
    private int sessionDurationHours = 24;
    
    // Security Headers Properties
    private String securityHeadersFrameOptions = "DENY";
    private boolean securityHeadersContentTypeOptions = true;
    private long securityHeadersHstsMaxAge = 31536000;
    private boolean securityHeadersHstsIncludeSubdomains = true;
    
    // Getters and Setters
    public String getJwtSecret() {
        return jwtSecret;
    }
    
    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }
    
    public long getJwtExpiration() {
        return jwtExpiration;
    }
    
    public void setJwtExpiration(long jwtExpiration) {
        this.jwtExpiration = jwtExpiration;
    }
    
    public int getPasswordMinLength() {
        return passwordMinLength;
    }
    
    public void setPasswordMinLength(int passwordMinLength) {
        this.passwordMinLength = passwordMinLength;
    }
    
    public int getPasswordMaxLength() {
        return passwordMaxLength;
    }
    
    public void setPasswordMaxLength(int passwordMaxLength) {
        this.passwordMaxLength = passwordMaxLength;
    }
    
    public boolean isPasswordRequireUppercase() {
        return passwordRequireUppercase;
    }
    
    public void setPasswordRequireUppercase(boolean passwordRequireUppercase) {
        this.passwordRequireUppercase = passwordRequireUppercase;
    }
    
    public boolean isPasswordRequireLowercase() {
        return passwordRequireLowercase;
    }
    
    public void setPasswordRequireLowercase(boolean passwordRequireLowercase) {
        this.passwordRequireLowercase = passwordRequireLowercase;
    }
    
    public boolean isPasswordRequireDigits() {
        return passwordRequireDigits;
    }
    
    public void setPasswordRequireDigits(boolean passwordRequireDigits) {
        this.passwordRequireDigits = passwordRequireDigits;
    }
    
    public boolean isPasswordRequireSpecialChars() {
        return passwordRequireSpecialChars;
    }
    
    public void setPasswordRequireSpecialChars(boolean passwordRequireSpecialChars) {
        this.passwordRequireSpecialChars = passwordRequireSpecialChars;
    }
    
    public int getRateLimitLoginAttemptsPerIp() {
        return rateLimitLoginAttemptsPerIp;
    }
    
    public void setRateLimitLoginAttemptsPerIp(int rateLimitLoginAttemptsPerIp) {
        this.rateLimitLoginAttemptsPerIp = rateLimitLoginAttemptsPerIp;
    }
    
    public int getRateLimitLoginAttemptsPerEmail() {
        return rateLimitLoginAttemptsPerEmail;
    }
    
    public void setRateLimitLoginAttemptsPerEmail(int rateLimitLoginAttemptsPerEmail) {
        this.rateLimitLoginAttemptsPerEmail = rateLimitLoginAttemptsPerEmail;
    }
    
    public int getRateLimitLockoutDurationMinutes() {
        return rateLimitLockoutDurationMinutes;
    }
    
    public void setRateLimitLockoutDurationMinutes(int rateLimitLockoutDurationMinutes) {
        this.rateLimitLockoutDurationMinutes = rateLimitLockoutDurationMinutes;
    }
    
    public int getSessionMaxConcurrent() {
        return sessionMaxConcurrent;
    }
    
    public void setSessionMaxConcurrent(int sessionMaxConcurrent) {
        this.sessionMaxConcurrent = sessionMaxConcurrent;
    }
    
    public int getSessionDurationHours() {
        return sessionDurationHours;
    }
    
    public void setSessionDurationHours(int sessionDurationHours) {
        this.sessionDurationHours = sessionDurationHours;
    }
    
    public String getSecurityHeadersFrameOptions() {
        return securityHeadersFrameOptions;
    }
    
    public void setSecurityHeadersFrameOptions(String securityHeadersFrameOptions) {
        this.securityHeadersFrameOptions = securityHeadersFrameOptions;
    }
    
    public boolean isSecurityHeadersContentTypeOptions() {
        return securityHeadersContentTypeOptions;
    }
    
    public void setSecurityHeadersContentTypeOptions(boolean securityHeadersContentTypeOptions) {
        this.securityHeadersContentTypeOptions = securityHeadersContentTypeOptions;
    }
    
    public long getSecurityHeadersHstsMaxAge() {
        return securityHeadersHstsMaxAge;
    }
    
    public void setSecurityHeadersHstsMaxAge(long securityHeadersHstsMaxAge) {
        this.securityHeadersHstsMaxAge = securityHeadersHstsMaxAge;
    }
    
    public boolean isSecurityHeadersHstsIncludeSubdomains() {
        return securityHeadersHstsIncludeSubdomains;
    }
    
    public void setSecurityHeadersHstsIncludeSubdomains(boolean securityHeadersHstsIncludeSubdomains) {
        this.securityHeadersHstsIncludeSubdomains = securityHeadersHstsIncludeSubdomains;
    }
}
