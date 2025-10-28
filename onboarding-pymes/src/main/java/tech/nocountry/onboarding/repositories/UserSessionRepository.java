package tech.nocountry.onboarding.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.nocountry.onboarding.entities.UserSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {
    
    // Buscar sesión por token hash
    Optional<UserSession> findByTokenHash(String tokenHash);
    
    // Buscar sesiones activas de un usuario
    List<UserSession> findByUserIdAndIsActiveTrue(String userId);
    
    // Buscar sesiones expiradas
    @Query("SELECT s FROM UserSession s WHERE s.expiresAt < :now AND s.isActive = true")
    List<UserSession> findExpiredSessions(@Param("now") LocalDateTime now);
    
    // Invalidar todas las sesiones de un usuario
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.userId = :userId")
    void invalidateAllUserSessions(@Param("userId") String userId);
    
    // Contar sesiones activas de un usuario
    long countByUserIdAndIsActiveTrue(String userId);
    
    // Buscar sesión por IP y User Agent (para detectar sesiones duplicadas)
    Optional<UserSession> findByIpAddressAndUserAgentAndIsActiveTrue(String ipAddress, String userAgent);
}
