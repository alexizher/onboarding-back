package tech.nocountry.onboarding.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.nocountry.onboarding.entities.ClientBlacklist;

import java.util.List;

@Repository
public interface ClientBlacklistRepository extends JpaRepository<ClientBlacklist, String> {
    
    // Buscar bloqueos activos de un usuario
    @Query("SELECT c FROM ClientBlacklist c WHERE c.userId = :userId AND c.isActive = true")
    List<ClientBlacklist> findActiveByUserId(@Param("userId") String userId);
    
    // Verificar si un usuario está bloqueado
    @Query("SELECT COUNT(c) > 0 FROM ClientBlacklist c WHERE c.userId = :userId AND c.isActive = true")
    boolean isUserBlacklisted(@Param("userId") String userId);
    
    // Buscar bloqueos activos de una aplicación específica
    @Query("SELECT c FROM ClientBlacklist c WHERE c.applicationId = :applicationId AND c.isActive = true")
    List<ClientBlacklist> findActiveByApplicationId(@Param("applicationId") String applicationId);
    
    // Buscar todos los bloqueos de un usuario (activos e inactivos)
    List<ClientBlacklist> findByUserIdOrderByBlacklistedAtDesc(String userId);
    
    // Buscar bloqueos activos creados por un usuario
    @Query("SELECT c FROM ClientBlacklist c WHERE c.blacklistedBy = :userId AND c.isActive = true")
    List<ClientBlacklist> findActiveCreatedBy(@Param("userId") String userId);
}

