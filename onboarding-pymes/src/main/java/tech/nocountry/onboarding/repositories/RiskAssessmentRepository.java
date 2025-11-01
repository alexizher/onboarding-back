package tech.nocountry.onboarding.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.nocountry.onboarding.entities.RiskAssessment;

import java.util.List;
import java.util.Optional;

@Repository
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, String> {

    Optional<RiskAssessment> findByAssessmentId(String assessmentId);

    @Query("SELECT ra FROM RiskAssessment ra WHERE ra.application.applicationId = :applicationId ORDER BY ra.assessedAt DESC")
    List<RiskAssessment> findByApplicationId(@Param("applicationId") String applicationId);

    @Query("SELECT ra FROM RiskAssessment ra WHERE ra.application.applicationId = :applicationId ORDER BY ra.assessedAt DESC")
    Optional<RiskAssessment> findLatestByApplicationId(@Param("applicationId") String applicationId);

    @Query("SELECT COUNT(ra) FROM RiskAssessment ra WHERE ra.level = :level")
    long countByLevel(@Param("level") String level);

    @Query("SELECT AVG(ra.score) FROM RiskAssessment ra")
    Double getAverageScore();

    @Query("SELECT AVG(ra.score) FROM RiskAssessment ra WHERE ra.application.applicationId = :applicationId")
    Double getAverageScoreByApplicationId(@Param("applicationId") String applicationId);
}

