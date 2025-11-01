package tech.nocountry.onboarding.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.nocountry.onboarding.entities.CreditApplication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CreditApplicationRepository extends JpaRepository<CreditApplication, String>, JpaSpecificationExecutor<CreditApplication> {

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

    // Fetch join para cargar el user junto con la aplicación (evita lazy loading)
    @Query("SELECT ca FROM CreditApplication ca LEFT JOIN FETCH ca.user WHERE ca.applicationId = :applicationId")
    Optional<CreditApplication> findByApplicationIdWithUser(@Param("applicationId") String applicationId);
    
    // Query para filtros avanzados con paginación (sin JOIN FETCH para compatibilidad con paginación)
    @Query(value = "SELECT ca FROM CreditApplication ca " +
                   "WHERE (:status IS NULL OR ca.status = :status) " +
                   "AND (:userId IS NULL OR ca.user.userId = :userId) " +
                   "AND (:assignedToUserId IS NULL OR ca.assignedTo.userId = :assignedToUserId) " +
                   "AND (:companyName IS NULL OR LOWER(ca.companyName) LIKE LOWER(CONCAT('%', :companyName, '%'))) " +
                   "AND (:cuit IS NULL OR ca.cuit = :cuit) " +
                   "AND (:createdFrom IS NULL OR ca.createdAt >= :createdFrom) " +
                   "AND (:createdTo IS NULL OR ca.createdAt <= :createdTo) " +
                   "AND (:minAmount IS NULL OR ca.amountRequested >= :minAmount) " +
                   "AND (:maxAmount IS NULL OR ca.amountRequested <= :maxAmount)",
           countQuery = "SELECT COUNT(ca) FROM CreditApplication ca " +
                       "WHERE (:status IS NULL OR ca.status = :status) " +
                       "AND (:userId IS NULL OR ca.user.userId = :userId) " +
                       "AND (:assignedToUserId IS NULL OR ca.assignedTo.userId = :assignedToUserId) " +
                       "AND (:companyName IS NULL OR LOWER(ca.companyName) LIKE LOWER(CONCAT('%', :companyName, '%'))) " +
                       "AND (:cuit IS NULL OR ca.cuit = :cuit) " +
                       "AND (:createdFrom IS NULL OR ca.createdAt >= :createdFrom) " +
                       "AND (:createdTo IS NULL OR ca.createdAt <= :createdTo) " +
                       "AND (:minAmount IS NULL OR ca.amountRequested >= :minAmount) " +
                       "AND (:maxAmount IS NULL OR ca.amountRequested <= :maxAmount)")
    Page<CreditApplication> findWithFilters(
            @Param("status") String status,
            @Param("userId") String userId,
            @Param("assignedToUserId") String assignedToUserId,
            @Param("companyName") String companyName,
            @Param("cuit") String cuit,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo,
            @Param("minAmount") java.math.BigDecimal minAmount,
            @Param("maxAmount") java.math.BigDecimal maxAmount,
            Pageable pageable);
}

