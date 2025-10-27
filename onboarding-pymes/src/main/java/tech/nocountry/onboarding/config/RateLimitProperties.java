package tech.nocountry.onboarding.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rate.limit")
public class RateLimitProperties {
    
    private int loginAttemptsPerIp = 5;
    private int loginAttemptsPerEmail = 3;
    private int lockoutDurationMinutes = 15;
    
    public int getLoginAttemptsPerIp() {
        return loginAttemptsPerIp;
    }
    
    public void setLoginAttemptsPerIp(int loginAttemptsPerIp) {
        this.loginAttemptsPerIp = loginAttemptsPerIp;
    }
    
    public int getLoginAttemptsPerEmail() {
        return loginAttemptsPerEmail;
    }
    
    public void setLoginAttemptsPerEmail(int loginAttemptsPerEmail) {
        this.loginAttemptsPerEmail = loginAttemptsPerEmail;
    }
    
    public int getLockoutDurationMinutes() {
        return lockoutDurationMinutes;
    }
    
    public void setLockoutDurationMinutes(int lockoutDurationMinutes) {
        this.lockoutDurationMinutes = lockoutDurationMinutes;
    }
}
