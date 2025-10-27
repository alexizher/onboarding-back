package tech.nocountry.onboarding.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.nocountry.onboarding.entities.ApplicationStatusHistory;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, String> {

    @Query("SELECT h FROM ApplicationStatusHistory h WHERE h.application.applicationId = :applicationId ORDER BY h.changedAt DESC")
    List<ApplicationStatusHistory> findByApplicationId(@Param("applicationId") String applicationId);

    @Query("SELECT h FROM ApplicationStatusHistory h WHERE h.application.applicationId = :applicationId ORDER BY h.changedAt DESC LIMIT 1")
    Optional<ApplicationStatusHistory> findLatestByApplicationId(@Param("applicationId") String applicationId);

    @Query("SELECT COUNT(h) FROM ApplicationStatusHistory h WHERE h.newStatus = :status")
    long countByStatus(@Param("status") String status);
}

