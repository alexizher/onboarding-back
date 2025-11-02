package tech.nocountry.onboarding.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "token_blacklist")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenBlacklist {
    
    @Id
    @Column(name = "token_id", length = 36)
    private String tokenId;
    
    @Column(name = "jti", nullable = false, length = 255, unique = true)
    private String jti; // JWT ID (JWT ID claim)
    
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;
    
    @Column(name = "blacklisted_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime blacklistedAt = LocalDateTime.now();
    
    @Column(name = "reason", length = 100)
    private String reason; // "LOGOUT", "PASSWORD_CHANGED", "SECURITY_VIOLATION", etc.
}

