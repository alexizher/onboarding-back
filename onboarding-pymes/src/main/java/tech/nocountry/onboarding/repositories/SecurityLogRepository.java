package tech.nocountry.onboarding.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.nocountry.onboarding.entities.SecurityLog;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SecurityLogRepository extends JpaRepository<SecurityLog, String> {
    
    // Buscar logs por usuario
    List<SecurityLog> findByUserIdOrderByTimestampDesc(String userId);
    
    // Buscar logs por tipo de evento
    List<SecurityLog> findByEventTypeOrderByTimestampDesc(String eventType);
    
    // Buscar logs por severidad
    List<SecurityLog> findBySeverityOrderByTimestampDesc(String severity);
    
    // Buscar logs por IP
    List<SecurityLog> findByIpAddressOrderByTimestampDesc(String ipAddress);
    
    // Buscar logs en un rango de tiempo
    @Query("SELECT s FROM SecurityLog s WHERE s.timestamp BETWEEN :start AND :end ORDER BY s.timestamp DESC")
    List<SecurityLog> findLogsInTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    // Buscar logs críticos recientes
    @Query("SELECT s FROM SecurityLog s WHERE s.severity = 'CRITICAL' AND s.timestamp > :since ORDER BY s.timestamp DESC")
    List<SecurityLog> findCriticalLogsSince(@Param("since") LocalDateTime since);
    
    // Contar eventos por tipo en un período
    @Query("SELECT s.eventType, COUNT(s) FROM SecurityLog s WHERE s.timestamp > :since GROUP BY s.eventType")
    List<Object[]> countEventsByTypeSince(@Param("since") LocalDateTime since);
    
    // Limpiar logs antiguos
    @Query("DELETE FROM SecurityLog s WHERE s.timestamp < :cutoff")
    void deleteOldLogs(@Param("cutoff") LocalDateTime cutoff);
}
