package tech.nocountry.onboarding.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "session")
public class SessionProperties {
    
    private int maxConcurrent = 3;
    private int durationHours = 24;
    
    public int getMaxConcurrent() {
        return maxConcurrent;
    }
    
    public void setMaxConcurrent(int maxConcurrent) {
        this.maxConcurrent = maxConcurrent;
    }
    
    public int getDurationHours() {
        return durationHours;
    }
    
    public void setDurationHours(int durationHours) {
        this.durationHours = durationHours;
    }
}
