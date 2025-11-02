package tech.nocountry.onboarding.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "client_blacklist")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientBlacklist {
    
    @Id
    @Column(name = "blacklist_id", length = 36)
    private String blacklistId;
    
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;
    
    @Column(name = "application_id", length = 36)
    private String applicationId; // Nullable - puede ser bloqueo general o específico a una aplicación
    
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;
    
    @Column(name = "blacklisted_by", nullable = false, length = 36)
    private String blacklistedBy; // Usuario que realizó el bloqueo (analista, manager, admin)
    
    @Column(name = "blacklisted_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime blacklistedAt = LocalDateTime.now();
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "unblacklisted_at")
    private LocalDateTime unblacklistedAt;
    
    @Column(name = "unblacklisted_by", length = 36)
    private String unblacklistedBy;
    
    @Column(name = "unblacklist_reason", columnDefinition = "TEXT")
    private String unblacklistReason;
}

