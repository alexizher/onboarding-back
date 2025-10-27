package tech.nocountry.onboarding.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAttempt {
    
    @Id
    @Column(name = "attempt_id", length = 36)
    private String attemptId;
    
    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;
    
    @Column(name = "email", length = 100)
    private String email;
    
    @Column(name = "attempt_time", nullable = false)
    @Builder.Default
    private LocalDateTime attemptTime = LocalDateTime.now();
    
    @Column(name = "successful", nullable = false)
    private Boolean successful;
    
    @Column(name = "failure_reason", length = 255)
    private String failureReason;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
}
