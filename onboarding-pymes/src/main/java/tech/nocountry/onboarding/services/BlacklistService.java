package tech.nocountry.onboarding.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.nocountry.onboarding.entities.ClientBlacklist;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.repositories.ClientBlacklistRepository;
import tech.nocountry.onboarding.repositories.UserRepository;
import tech.nocountry.onboarding.services.SecurityAuditService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BlacklistService {

    private final ClientBlacklistRepository clientBlacklistRepository;
    private final UserRepository userRepository;
    private final SecurityAuditService securityAuditService;

    /**
     * Bloquear un usuario
     */
    public ClientBlacklist blacklistUser(String userId, String applicationId, String reason, String blacklistedBy, String ipAddress, String userAgent) {
        log.info("Blacklisting user: {} by: {}", userId, blacklistedBy);
        
        // Verificar que el usuario existe
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("Usuario no encontrado");
        }
        
        // Verificar si ya está bloqueado
        if (clientBlacklistRepository.isUserBlacklisted(userId)) {
            log.warn("User {} is already blacklisted", userId);
            throw new RuntimeException("Usuario ya está bloqueado");
        }
        
        // Crear entrada de blacklist
        ClientBlacklist blacklist = ClientBlacklist.builder()
                .blacklistId(UUID.randomUUID().toString())
                .userId(userId)
                .applicationId(applicationId) // Puede ser null para bloqueo general
                .reason(reason)
                .blacklistedBy(blacklistedBy)
                .isActive(true)
                .build();
        
        ClientBlacklist saved = clientBlacklistRepository.save(blacklist);
        
        // Log de seguridad
        securityAuditService.logSecurityEvent(
            userId,
            "USER_BLACKLISTED",
            ipAddress,
            userAgent,
            "Usuario bloqueado por: " + blacklistedBy + ". Razón: " + reason,
            "HIGH"
        );
        
        log.info("User {} blacklisted successfully", userId);
        return saved;
    }
    
    /**
     * Desbloquear un usuario
     */
    public boolean unblacklistUser(String userId, String unblacklistedBy, String reason, String ipAddress, String userAgent) {
        log.info("Unblacklisting user: {} by: {}", userId, unblacklistedBy);
        
        List<ClientBlacklist> activeBlacklists = clientBlacklistRepository.findActiveByUserId(userId);
        
        if (activeBlacklists.isEmpty()) {
            log.warn("User {} is not blacklisted", userId);
            return false;
        }
        
        // Desbloquear todos los bloqueos activos
        for (ClientBlacklist blacklist : activeBlacklists) {
            blacklist.setIsActive(false);
            blacklist.setUnblacklistedAt(LocalDateTime.now());
            blacklist.setUnblacklistedBy(unblacklistedBy);
            blacklist.setUnblacklistReason(reason);
            clientBlacklistRepository.save(blacklist);
        }
        
        // Log de seguridad
        securityAuditService.logSecurityEvent(
            userId,
            "USER_UNBLACKLISTED",
            ipAddress,
            userAgent,
            "Usuario desbloqueado por: " + unblacklistedBy + ". Razón: " + reason,
            "MEDIUM"
        );
        
        log.info("User {} unblacklisted successfully", userId);
        return true;
    }
    
    /**
     * Verificar si un usuario está bloqueado
     */
    @Transactional(readOnly = true)
    public boolean isUserBlacklisted(String userId) {
        return clientBlacklistRepository.isUserBlacklisted(userId);
    }
    
    /**
     * Obtener bloqueos activos de un usuario
     */
    @Transactional(readOnly = true)
    public List<ClientBlacklist> getUserActiveBlacklists(String userId) {
        return clientBlacklistRepository.findActiveByUserId(userId);
    }
    
    /**
     * Obtener historial de bloqueos de un usuario
     */
    @Transactional(readOnly = true)
    public List<ClientBlacklist> getUserBlacklistHistory(String userId) {
        return clientBlacklistRepository.findByUserIdOrderByBlacklistedAtDesc(userId);
    }
}

