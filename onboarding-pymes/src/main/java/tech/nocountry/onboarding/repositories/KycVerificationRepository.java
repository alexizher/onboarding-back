package tech.nocountry.onboarding.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.nocountry.onboarding.entities.KycVerification;

import java.util.List;
import java.util.Optional;

@Repository
public interface KycVerificationRepository extends JpaRepository<KycVerification, String> {

    Optional<KycVerification> findByVerificationId(String verificationId);

    @Query("SELECT kv FROM KycVerification kv WHERE kv.application.applicationId = :applicationId ORDER BY kv.createdAt DESC")
    List<KycVerification> findByApplicationId(@Param("applicationId") String applicationId);

    @Query("SELECT kv FROM KycVerification kv WHERE kv.user.userId = :userId ORDER BY kv.createdAt DESC")
    List<KycVerification> findByUserId(@Param("userId") String userId);

    @Query("SELECT kv FROM KycVerification kv WHERE kv.application.applicationId = :applicationId AND kv.status = :status ORDER BY kv.createdAt DESC")
    List<KycVerification> findByApplicationIdAndStatus(@Param("applicationId") String applicationId, @Param("status") String status);

    @Query("SELECT kv FROM KycVerification kv WHERE kv.application.applicationId = :applicationId AND kv.verificationType = :verificationType ORDER BY kv.createdAt DESC")
    Optional<KycVerification> findLatestByApplicationIdAndVerificationType(
            @Param("applicationId") String applicationId, 
            @Param("verificationType") String verificationType);

    @Query("SELECT COUNT(kv) FROM KycVerification kv WHERE kv.status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT COUNT(kv) FROM KycVerification kv WHERE kv.provider = :provider")
    long countByProvider(@Param("provider") String provider);
}

