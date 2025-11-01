package tech.nocountry.onboarding.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.nocountry.onboarding.entities.Document;

import java.time.LocalDateTime;
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

    boolean existsByHash(String hash);

    Optional<Document> findByHash(String hash);
    
    // Query para filtros avanzados con paginación
    @Query(value = "SELECT d FROM Document d " +
                   "WHERE (:verificationStatus IS NULL OR d.verificationStatus = :verificationStatus) " +
                   "AND (:documentTypeId IS NULL OR d.documentType.documentTypeId = :documentTypeId) " +
                   "AND (:applicationId IS NULL OR d.application.applicationId = :applicationId) " +
                   "AND (:userId IS NULL OR d.user.userId = :userId) " +
                   "AND (:verifiedByUserId IS NULL OR d.verifiedBy.userId = :verifiedByUserId) " +
                   "AND (:fileName IS NULL OR LOWER(d.fileName) LIKE LOWER(CONCAT('%', :fileName, '%'))) " +
                   "AND (:uploadedFrom IS NULL OR d.uploadedAt >= :uploadedFrom) " +
                   "AND (:uploadedTo IS NULL OR d.uploadedAt <= :uploadedTo) " +
                   "AND (:verifiedFrom IS NULL OR d.verifiedAt >= :verifiedFrom) " +
                   "AND (:verifiedTo IS NULL OR d.verifiedAt <= :verifiedTo)",
           countQuery = "SELECT COUNT(d) FROM Document d " +
                       "WHERE (:verificationStatus IS NULL OR d.verificationStatus = :verificationStatus) " +
                       "AND (:documentTypeId IS NULL OR d.documentType.documentTypeId = :documentTypeId) " +
                       "AND (:applicationId IS NULL OR d.application.applicationId = :applicationId) " +
                       "AND (:userId IS NULL OR d.user.userId = :userId) " +
                       "AND (:verifiedByUserId IS NULL OR d.verifiedBy.userId = :verifiedByUserId) " +
                       "AND (:fileName IS NULL OR LOWER(d.fileName) LIKE LOWER(CONCAT('%', :fileName, '%'))) " +
                       "AND (:uploadedFrom IS NULL OR d.uploadedAt >= :uploadedFrom) " +
                       "AND (:uploadedTo IS NULL OR d.uploadedAt <= :uploadedTo) " +
                       "AND (:verifiedFrom IS NULL OR d.verifiedAt >= :verifiedFrom) " +
                       "AND (:verifiedTo IS NULL OR d.verifiedAt <= :verifiedTo)")
    Page<Document> findWithFilters(
            @Param("verificationStatus") String verificationStatus,
            @Param("documentTypeId") String documentTypeId,
            @Param("applicationId") String applicationId,
            @Param("userId") String userId,
            @Param("verifiedByUserId") String verifiedByUserId,
            @Param("fileName") String fileName,
            @Param("uploadedFrom") LocalDateTime uploadedFrom,
            @Param("uploadedTo") LocalDateTime uploadedTo,
            @Param("verifiedFrom") LocalDateTime verifiedFrom,
            @Param("verifiedTo") LocalDateTime verifiedTo,
            Pageable pageable);
    
    // Documentos pendientes de verificación
    @Query("SELECT d FROM Document d WHERE d.verificationStatus = 'pending' ORDER BY d.uploadedAt DESC")
    Page<Document> findPendingDocuments(Pageable pageable);
    
    // Contar documentos por estado de verificación
    @Query("SELECT COUNT(d) FROM Document d WHERE d.verificationStatus = :status")
    long countByVerificationStatus(@Param("status") String status);
}
