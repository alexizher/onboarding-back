package tech.nocountry.onboarding.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.nocountry.onboarding.entities.PasswordResetToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {
    
    // Buscar token por valor
    Optional<PasswordResetToken> findByToken(String token);
    
    // Buscar tokens válidos de un usuario
    @Query("SELECT t FROM PasswordResetToken t WHERE t.userId = :userId AND t.used = false AND t.expiresAt > :now")
    List<PasswordResetToken> findValidTokensByUserId(@Param("userId") String userId, @Param("now") LocalDateTime now);
    
    // Buscar tokens expirados
    @Query("SELECT t FROM PasswordResetToken t WHERE t.expiresAt < :now")
    List<PasswordResetToken> findExpiredTokens(@Param("now") LocalDateTime now);
    
    // Marcar token como usado
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.token = :token")
    void markTokenAsUsed(@Param("token") String token);
    
    // Invalidar todos los tokens de un usuario
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.userId = :userId AND t.used = false")
    void invalidateAllUserTokens(@Param("userId") String userId);
}
