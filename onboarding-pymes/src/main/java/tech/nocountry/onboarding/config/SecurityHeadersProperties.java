package tech.nocountry.onboarding.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security.headers")
public class SecurityHeadersProperties {
    
    private String frameOptions = "DENY";
    private boolean contentTypeOptions = true;
    private long hstsMaxAge = 31536000;
    private boolean hstsIncludeSubdomains = true;
    
    public String getFrameOptions() {
        return frameOptions;
    }
    
    public void setFrameOptions(String frameOptions) {
        this.frameOptions = frameOptions;
    }
    
    public boolean isContentTypeOptions() {
        return contentTypeOptions;
    }
    
    public void setContentTypeOptions(boolean contentTypeOptions) {
        this.contentTypeOptions = contentTypeOptions;
    }
    
    public long getHstsMaxAge() {
        return hstsMaxAge;
    }
    
    public void setHstsMaxAge(long hstsMaxAge) {
        this.hstsMaxAge = hstsMaxAge;
    }
    
    public boolean isHstsIncludeSubdomains() {
        return hstsIncludeSubdomains;
    }
    
    public void setHstsIncludeSubdomains(boolean hstsIncludeSubdomains) {
        this.hstsIncludeSubdomains = hstsIncludeSubdomains;
    }
}
