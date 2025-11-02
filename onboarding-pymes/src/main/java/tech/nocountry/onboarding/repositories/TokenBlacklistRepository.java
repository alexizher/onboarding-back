package tech.nocountry.onboarding.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.nocountry.onboarding.entities.TokenBlacklist;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, String> {
    
    // Buscar token por JWT ID
    Optional<TokenBlacklist> findByJti(String jti);
    
    // Verificar si un token está en la blacklist
    boolean existsByJti(String jti);
    
    // Buscar tokens blacklisted de un usuario
    @Query("SELECT t FROM TokenBlacklist t WHERE t.userId = :userId ORDER BY t.blacklistedAt DESC")
    java.util.List<TokenBlacklist> findByUserIdOrderByBlacklistedAtDesc(@Param("userId") String userId);
    
    // Limpiar tokens blacklisted antiguos (más de X días, útil si los tokens expiran rápido)
    @Query("DELETE FROM TokenBlacklist t WHERE t.blacklistedAt < :cutoff")
    void deleteOldTokens(@Param("cutoff") LocalDateTime cutoff);
}

