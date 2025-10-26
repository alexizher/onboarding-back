package tech.nocountry.onboarding.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.nocountry.onboarding.entities.CreditApplication;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreditApplicationRepository extends JpaRepository<CreditApplication, String> {

    Optional<CreditApplication> findByApplicationId(String applicationId);

    @Query("SELECT ca FROM CreditApplication ca WHERE ca.user.userId = :userId")
    List<CreditApplication> findByUserId(@Param("userId") String userId);

    List<CreditApplication> findByStatus(String status);

    @Query("SELECT ca FROM CreditApplication ca WHERE ca.assignedTo.userId = :assignedTo")
    List<CreditApplication> findByAssignedTo(@Param("assignedTo") String assignedTo);

    @Query("SELECT ca FROM CreditApplication ca WHERE ca.status = :status")
    List<CreditApplication> findAllByStatus(@Param("status") String status);

    @Query("SELECT ca FROM CreditApplication ca WHERE ca.user.userId = :userId AND ca.status = :status")
    List<CreditApplication> findByUserIdAndStatus(@Param("userId") String userId, @Param("status") String status);

    @Query("SELECT ca FROM CreditApplication ca WHERE ca.assignedTo.userId = :assignedTo AND ca.status = :status")
    List<CreditApplication> findByAssignedToAndStatus(@Param("assignedTo") String assignedTo, @Param("status") String status);

    @Query("SELECT COUNT(ca) FROM CreditApplication ca WHERE ca.status = :status")
    long countByStatus(@Param("status") String status);
}

