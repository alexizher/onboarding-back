package tech.nocountry.onboarding.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.nocountry.onboarding.entities.SmsNotification;

import java.util.List;

@Repository
public interface SmsNotificationRepository extends JpaRepository<SmsNotification, String> {
    
    @Query("SELECT s FROM SmsNotification s WHERE s.userId = :userId ORDER BY s.createdAt DESC")
    List<SmsNotification> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId);
    
    @Query("SELECT s FROM SmsNotification s WHERE s.userId = :userId AND s.isRead = false ORDER BY s.createdAt DESC")
    List<SmsNotification> findUnreadByUserId(@Param("userId") String userId);
    
    @Query("SELECT s FROM SmsNotification s WHERE s.userId = :userId AND s.notificationType = :type ORDER BY s.createdAt DESC")
    List<SmsNotification> findByUserIdAndType(@Param("userId") String userId, @Param("type") String type);
    
    @Query("SELECT s FROM SmsNotification s WHERE s.isSent = false AND s.retryCount < :maxRetries ORDER BY s.createdAt ASC")
    List<SmsNotification> findPendingNotifications(@Param("maxRetries") int maxRetries);
    
    @Query("SELECT COUNT(s) FROM SmsNotification s WHERE s.userId = :userId AND s.isRead = false")
    long countUnreadByUserId(@Param("userId") String userId);
}

