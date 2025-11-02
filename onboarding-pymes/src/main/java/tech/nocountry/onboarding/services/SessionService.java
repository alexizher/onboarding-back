package tech.nocountry.onboarding.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.nocountry.onboarding.config.SessionProperties;
import tech.nocountry.onboarding.entities.UserSession;
import tech.nocountry.onboarding.repositories.UserSessionRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class SessionService {

    @Autowired
    private UserSessionRepository sessionRepository;

    @Autowired
    private SessionProperties sessionProperties;

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
        // Usar SHA-256 para hash del token JWT (que es muy largo para BCrypt)
        String tokenHash = hashToken(token);
        // Duración corta para sistema bancario (30 minutos por defecto)
        double durationHours = sessionProperties != null ? sessionProperties.getDurationHours() : 0.5;
        int durationMinutes = (int)(durationHours * 60); // Convertir horas a minutos
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(durationMinutes);

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
     * Generar hash SHA-256 de un token JWT
     */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar hash de token", e);
        }
    }

    /**
     * Validar una sesión por token (incluye verificación de inactividad)
     */
    public boolean validateSession(String token) {
        String tokenHash = hashToken(token);
        LocalDateTime now = LocalDateTime.now();
        
        // Buscar en todas las sesiones activas
        for (UserSession session : sessionRepository.findAll()) {
            if (session.getIsActive() && 
                session.getExpiresAt().isAfter(now) &&
                tokenHash.equals(session.getTokenHash())) {
                
                // Verificar timeout de inactividad (sistema bancario)
                if (session.getLastActivity() != null) {
                    int inactivityTimeout = sessionProperties != null ? 
                        sessionProperties.getInactivityTimeoutMinutes() : 15;
                    LocalDateTime inactivityThreshold = now.minusMinutes(inactivityTimeout);
                    if (session.getLastActivity().isBefore(inactivityThreshold)) {
                        // Sesión expirada por inactividad
                        session.setIsActive(false);
                        sessionRepository.save(session);
                        return false;
                    }
                }
                
                // Actualizar última actividad
                session.setLastActivity(now);
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
        String tokenHash = hashToken(token);
        for (UserSession session : sessionRepository.findAll()) {
            if (tokenHash.equals(session.getTokenHash())) {
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
     * Invalidar una sesión específica por sessionId
     */
    public boolean invalidateSessionById(String sessionId, String userId) {
        Optional<UserSession> sessionOptional = sessionRepository.findById(sessionId);
        
        if (sessionOptional.isEmpty()) {
            return false;
        }
        
        UserSession session = sessionOptional.get();
        
        // Verificar que la sesión pertenece al usuario
        if (!session.getUserId().equals(userId)) {
            return false;
        }
        
        // Invalidar la sesión
        session.setIsActive(false);
        sessionRepository.save(session);
        
        return true;
    }

    /**
     * Cerrar todas las demás sesiones excepto la actual
     */
    public int closeOtherSessions(String userId, String currentTokenHash) {
        List<UserSession> activeSessions = sessionRepository.findByUserIdAndIsActiveTrue(userId);
        
        int closedCount = 0;
        for (UserSession session : activeSessions) {
            // No invalidar la sesión actual
            if (!currentTokenHash.equals(session.getTokenHash())) {
                session.setIsActive(false);
                sessionRepository.save(session);
                closedCount++;
            }
        }
        
        return closedCount;
    }

    /**
     * Obtener sesiones activas de un usuario
     */
    public List<UserSession> getUserActiveSessions(String userId) {
        return sessionRepository.findByUserIdAndIsActiveTrue(userId);
    }

    /**
     * Limpiar sesiones expiradas y por inactividad (ejecutar periódicamente)
     */
    @Scheduled(fixedRate = 300000) // Cada 5 minutos para sistema bancario
    public void cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        int inactivityTimeout = sessionProperties != null ? 
            sessionProperties.getInactivityTimeoutMinutes() : 15;
        LocalDateTime inactivityThreshold = now.minusMinutes(inactivityTimeout);
        
        // Limpiar sesiones expiradas por tiempo
        List<UserSession> expiredSessions = sessionRepository.findExpiredSessions(now);
        for (UserSession session : expiredSessions) {
            session.setIsActive(false);
        }
        
        // Limpiar sesiones por inactividad
        List<UserSession> activeSessions = sessionRepository.findAll().stream()
            .filter(s -> s.getIsActive() != null && s.getIsActive())
            .filter(s -> s.getLastActivity() != null && s.getLastActivity().isBefore(inactivityThreshold))
            .toList();
        
        for (UserSession session : activeSessions) {
            session.setIsActive(false);
        }
        
        if (!expiredSessions.isEmpty() || !activeSessions.isEmpty()) {
            sessionRepository.saveAll(expiredSessions);
            sessionRepository.saveAll(activeSessions);
        }
    }

    /**
     * Verificar si hay sesiones duplicadas (misma IP y User Agent)
     */
    public boolean hasDuplicateSession(String ipAddress, String userAgent) {
        return sessionRepository.findByIpAddressAndUserAgentAndIsActiveTrue(ipAddress, userAgent).isPresent();
    }
}
