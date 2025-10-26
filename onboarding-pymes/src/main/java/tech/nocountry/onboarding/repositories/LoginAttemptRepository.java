package tech.nocountry.onboarding.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.nocountry.onboarding.entities.LoginAttempt;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, String> {
    
    // Contar intentos fallidos por IP en un período de tiempo
    @Query("SELECT COUNT(l) FROM LoginAttempt l WHERE l.ipAddress = :ipAddress AND l.successful = false AND l.attemptTime > :since")
    long countFailedAttemptsByIpSince(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);
    
    // Contar intentos fallidos por email en un período de tiempo
    @Query("SELECT COUNT(l) FROM LoginAttempt l WHERE l.email = :email AND l.successful = false AND l.attemptTime > :since")
    long countFailedAttemptsByEmailSince(@Param("email") String email, @Param("since") LocalDateTime since);
    
    // Obtener últimos intentos de una IP
    List<LoginAttempt> findTop10ByIpAddressOrderByAttemptTimeDesc(String ipAddress);
    
    // Obtener últimos intentos de un email
    List<LoginAttempt> findTop10ByEmailOrderByAttemptTimeDesc(String email);
    
    // Limpiar intentos antiguos
    @Query("DELETE FROM LoginAttempt l WHERE l.attemptTime < :cutoff")
    void deleteOldAttempts(@Param("cutoff") LocalDateTime cutoff);
    
    // Verificar si una IP está bloqueada (más de X intentos en Y minutos)
    @Query("SELECT COUNT(l) FROM LoginAttempt l WHERE l.ipAddress = :ipAddress AND l.successful = false AND l.attemptTime > :since")
    long isIpBlocked(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);
}
