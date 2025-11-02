package tech.nocountry.onboarding.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.nocountry.onboarding.entities.EmailVerificationToken;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, String> {
    
    Optional<EmailVerificationToken> findByToken(String token);
    
    Optional<EmailVerificationToken> findByUserIdAndIsUsedFalse(String userId);
    
    @Modifying
    @Query("UPDATE EmailVerificationToken e SET e.isUsed = true, e.verifiedAt = :verifiedAt WHERE e.userId = :userId AND e.isUsed = false")
    void invalidateAllUserTokens(@Param("userId") String userId, @Param("verifiedAt") LocalDateTime verifiedAt);
    
    @Query("SELECT e FROM EmailVerificationToken e WHERE e.token = :token AND e.isUsed = false AND e.expiresAt > :now")
    Optional<EmailVerificationToken> findValidToken(@Param("token") String token, @Param("now") LocalDateTime now);
}

