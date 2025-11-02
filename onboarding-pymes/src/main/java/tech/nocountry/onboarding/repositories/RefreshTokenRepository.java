package tech.nocountry.onboarding.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.nocountry.onboarding.entities.RefreshToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    
    Optional<RefreshToken> findByToken(String token);
    
    @Query("SELECT r FROM RefreshToken r WHERE r.userId = :userId AND r.isRevoked = false AND r.expiresAt > :now")
    List<RefreshToken> findActiveTokensByUserId(@Param("userId") String userId, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE RefreshToken r SET r.isRevoked = true, r.revokedAt = :revokedAt WHERE r.userId = :userId AND r.isRevoked = false")
    void revokeAllUserTokens(@Param("userId") String userId, @Param("revokedAt") LocalDateTime revokedAt);
    
    @Modifying
    @Query("UPDATE RefreshToken r SET r.isRevoked = true, r.revokedAt = :revokedAt WHERE r.tokenHash = :tokenHash")
    void revokeToken(@Param("tokenHash") String tokenHash, @Param("revokedAt") LocalDateTime revokedAt);
    
    @Query("SELECT r FROM RefreshToken r WHERE r.isRevoked = false AND r.expiresAt < :now")
    List<RefreshToken> findExpiredTokens(@Param("now") LocalDateTime now);
}

