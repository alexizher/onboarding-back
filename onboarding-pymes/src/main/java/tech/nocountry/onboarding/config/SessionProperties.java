package tech.nocountry.onboarding.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "session")
public class SessionProperties {
    
    private int maxConcurrent = 3;
    private double durationHours = 0.5; // 30 minutos por defecto para sistema bancario
    private int inactivityTimeoutMinutes = 15; // Timeout de inactividad
    
    public int getMaxConcurrent() {
        return maxConcurrent;
    }
    
    public void setMaxConcurrent(int maxConcurrent) {
        this.maxConcurrent = maxConcurrent;
    }
    
    public double getDurationHours() {
        return durationHours;
    }
    
    public void setDurationHours(double durationHours) {
        this.durationHours = durationHours;
    }
    
    public int getInactivityTimeoutMinutes() {
        return inactivityTimeoutMinutes;
    }
    
    public void setInactivityTimeoutMinutes(int inactivityTimeoutMinutes) {
        this.inactivityTimeoutMinutes = inactivityTimeoutMinutes;
    }
}
