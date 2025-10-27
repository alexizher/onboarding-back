package tech.nocountry.onboarding.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.nocountry.onboarding.entities.Document;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {

    Optional<Document> findByDocumentId(String documentId);

    @Query("SELECT d FROM Document d WHERE d.application.applicationId = :applicationId")
    List<Document> findByApplicationId(@Param("applicationId") String applicationId);

    @Query("SELECT d FROM Document d WHERE d.user.userId = :userId")
    List<Document> findByUserId(@Param("userId") String userId);

    @Query("SELECT d FROM Document d WHERE d.verificationStatus = :status")
    List<Document> findByVerificationStatus(@Param("status") String status);

    @Query("SELECT d FROM Document d WHERE d.application.applicationId = :applicationId AND d.verificationStatus = :status")
    List<Document> findByApplicationIdAndVerificationStatus(@Param("applicationId") String applicationId, @Param("status") String status);
}
