package tech.nocountry.onboarding.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.nocountry.onboarding.entities.PasswordHistory;

import java.util.List;

@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, String> {
    
    @Query("SELECT p FROM PasswordHistory p WHERE p.userId = :userId ORDER BY p.createdAt DESC")
    List<PasswordHistory> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId);
    
    @Query("SELECT COUNT(p) FROM PasswordHistory p WHERE p.userId = :userId AND p.passwordHash = :passwordHash")
    long countByUserIdAndPasswordHash(@Param("userId") String userId, @Param("passwordHash") String passwordHash);
    
    @Query("DELETE FROM PasswordHistory p WHERE p.userId = :userId AND p.createdAt NOT IN (SELECT p2.createdAt FROM PasswordHistory p2 WHERE p2.userId = :userId ORDER BY p2.createdAt DESC LIMIT :limit)")
    void deleteOldPasswords(@Param("userId") String userId, @Param("limit") int limit);
}

