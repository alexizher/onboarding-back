package tech.nocountry.onboarding.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.nocountry.onboarding.entities.UserSession;
import tech.nocountry.onboarding.repositories.UserSessionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SessionService {

    @Autowired
    private UserSessionRepository sessionRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Duración de sesión en horas
    private static final int SESSION_DURATION_HOURS = 24;

    /**
     * Crear una nueva sesión para un usuario
     */
    public UserSession createSession(String userId, String token, String ipAddress, String userAgent) {
        // Invalidar sesiones existentes del usuario si tiene más de 3 activas
        long activeSessions = sessionRepository.countByUserIdAndIsActiveTrue(userId);
        if (activeSessions >= 3) {
            sessionRepository.invalidateAllUserSessions(userId);
        }

        String sessionId = UUID.randomUUID().toString();
        String tokenHash = passwordEncoder.encode(token);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(SESSION_DURATION_HOURS);

        UserSession session = UserSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .lastActivity(LocalDateTime.now())
                .isActive(true)
                .build();

        return sessionRepository.save(session);
    }

    /**
     * Validar una sesión por token
     */
    public boolean validateSession(String token) {
        // Buscar en todas las sesiones activas
        for (UserSession session : sessionRepository.findAll()) {
            if (session.getIsActive() && 
                session.getExpiresAt().isAfter(LocalDateTime.now()) &&
                passwordEncoder.matches(token, session.getTokenHash())) {
                // Actualizar última actividad
                session.setLastActivity(LocalDateTime.now());
                sessionRepository.save(session);
                return true;
            }
        }
        return false;
    }

    /**
     * Invalidar una sesión específica
     */
    public boolean invalidateSession(String token) {
        for (UserSession session : sessionRepository.findAll()) {
            if (passwordEncoder.matches(token, session.getTokenHash())) {
                session.setIsActive(false);
                sessionRepository.save(session);
                return true;
            }
        }
        return false;
    }

    /**
     * Invalidar todas las sesiones de un usuario
     */
    public void invalidateAllUserSessions(String userId) {
        sessionRepository.invalidateAllUserSessions(userId);
    }

    /**
     * Obtener sesiones activas de un usuario
     */
    public List<UserSession> getUserActiveSessions(String userId) {
        return sessionRepository.findByUserIdAndIsActiveTrue(userId);
    }

    /**
     * Limpiar sesiones expiradas (ejecutar periódicamente)
     */
    @Scheduled(fixedRate = 3600000) // Cada hora
    public void cleanupExpiredSessions() {
        List<UserSession> expiredSessions = sessionRepository.findExpiredSessions(LocalDateTime.now());
        for (UserSession session : expiredSessions) {
            session.setIsActive(false);
        }
        sessionRepository.saveAll(expiredSessions);
    }

    /**
     * Verificar si hay sesiones duplicadas (misma IP y User Agent)
     */
    public boolean hasDuplicateSession(String ipAddress, String userAgent) {
        return sessionRepository.findByIpAddressAndUserAgentAndIsActiveTrue(ipAddress, userAgent).isPresent();
    }
}
