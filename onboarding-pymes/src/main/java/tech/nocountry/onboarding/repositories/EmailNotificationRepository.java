package tech.nocountry.onboarding.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.nocountry.onboarding.entities.EmailNotification;

import java.util.List;

@Repository
public interface EmailNotificationRepository extends JpaRepository<EmailNotification, String> {
    
    @Query("SELECT e FROM EmailNotification e WHERE e.userId = :userId ORDER BY e.createdAt DESC")
    List<EmailNotification> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId);
    
    @Query("SELECT e FROM EmailNotification e WHERE e.userId = :userId AND e.isRead = false ORDER BY e.createdAt DESC")
    List<EmailNotification> findUnreadByUserId(@Param("userId") String userId);
    
    @Query("SELECT e FROM EmailNotification e WHERE e.userId = :userId AND e.notificationType = :type ORDER BY e.createdAt DESC")
    List<EmailNotification> findByUserIdAndType(@Param("userId") String userId, @Param("type") String type);
    
    @Query("SELECT e FROM EmailNotification e WHERE e.isSent = false AND e.retryCount < :maxRetries ORDER BY e.createdAt ASC")
    List<EmailNotification> findPendingNotifications(@Param("maxRetries") int maxRetries);
    
    @Query("SELECT COUNT(e) FROM EmailNotification e WHERE e.userId = :userId AND e.isRead = false")
    long countUnreadByUserId(@Param("userId") String userId);
}

