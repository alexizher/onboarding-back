package tech.nocountry.onboarding.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "password")
public class PasswordProperties {
    
    private int minLength = 8;
    private int maxLength = 128;
    private boolean requireUppercase = true;
    private boolean requireLowercase = true;
    private boolean requireDigits = true;
    private boolean requireSpecialChars = false;
    
    public int getMinLength() {
        return minLength;
    }
    
    public void setMinLength(int minLength) {
        this.minLength = minLength;
    }
    
    public int getMaxLength() {
        return maxLength;
    }
    
    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }
    
    public boolean isRequireUppercase() {
        return requireUppercase;
    }
    
    public void setRequireUppercase(boolean requireUppercase) {
        this.requireUppercase = requireUppercase;
    }
    
    public boolean isRequireLowercase() {
        return requireLowercase;
    }
    
    public void setRequireLowercase(boolean requireLowercase) {
        this.requireLowercase = requireLowercase;
    }
    
    public boolean isRequireDigits() {
        return requireDigits;
    }
    
    public void setRequireDigits(boolean requireDigits) {
        this.requireDigits = requireDigits;
    }
    
    public boolean isRequireSpecialChars() {
        return requireSpecialChars;
    }
    
    public void setRequireSpecialChars(boolean requireSpecialChars) {
        this.requireSpecialChars = requireSpecialChars;
    }
}
