package tech.nocountry.onboarding.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityLog {
    
    @Id
    @Column(name = "log_id", length = 36)
    private String logId;
    
    @Column(name = "user_id", length = 36)
    private String userId;
    
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType; // LOGIN_SUCCESS, LOGIN_FAILED, PASSWORD_CHANGE, etc.
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;
    
    @Column(name = "timestamp", nullable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    @Column(name = "severity", nullable = false, length = 20)
    @Builder.Default
    private String severity = "MEDIUM"; // LOW, MEDIUM, HIGH, CRITICAL
}
