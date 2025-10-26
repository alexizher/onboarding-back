package tech.nocountry.onboarding.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.nocountry.onboarding.entities.SecurityLog;
import tech.nocountry.onboarding.repositories.SecurityLogRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class SecurityAuditService {

    @Autowired
    private SecurityLogRepository securityLogRepository;

    /**
     * Registrar evento de seguridad
     */
    public void logSecurityEvent(String userId, String eventType, String ipAddress, 
                                String userAgent, String details, String severity) {
        SecurityLog log = SecurityLog.builder()
                .logId(java.util.UUID.randomUUID().toString())
                .userId(userId)
                .eventType(eventType)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .details(details)
                .severity(severity)
                .timestamp(LocalDateTime.now())
                .build();

        securityLogRepository.save(log);
    }

    /**
     * Registrar evento de seguridad con severidad por defecto
     */
    public void logSecurityEvent(String userId, String eventType, String ipAddress, 
                                String userAgent, String details) {
        logSecurityEvent(userId, eventType, ipAddress, userAgent, details, "MEDIUM");
    }

    /**
     * Obtener logs de un usuario
     */
    public List<SecurityLog> getUserLogs(String userId) {
        return securityLogRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    /**
     * Obtener logs por tipo de evento
     */
    public List<SecurityLog> getLogsByEventType(String eventType) {
        return securityLogRepository.findByEventTypeOrderByTimestampDesc(eventType);
    }

    /**
     * Obtener logs críticos recientes
     */
    public List<SecurityLog> getCriticalLogs() {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        return securityLogRepository.findCriticalLogsSince(oneDayAgo);
    }

    /**
     * Obtener logs por IP
     */
    public List<SecurityLog> getLogsByIp(String ipAddress) {
        return securityLogRepository.findByIpAddressOrderByTimestampDesc(ipAddress);
    }

    /**
     * Obtener estadísticas de eventos
     */
    public List<Object[]> getEventStatistics() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        return securityLogRepository.countEventsByTypeSince(oneWeekAgo);
    }

    /**
     * Detectar patrones sospechosos
     */
    public List<SecurityLog> detectSuspiciousActivity() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        
        // Aquí se podría implementar lógica más compleja para detectar patrones
        // Por ahora, retornamos logs críticos recientes
        return securityLogRepository.findCriticalLogsSince(oneHourAgo);
    }

    /**
     * Generar reporte de seguridad
     */
    public String generateSecurityReport() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        List<Object[]> eventStats = securityLogRepository.countEventsByTypeSince(oneWeekAgo);
        List<SecurityLog> criticalLogs = securityLogRepository.findCriticalLogsSince(oneWeekAgo);
        
        StringBuilder report = new StringBuilder();
        report.append("=== REPORTE DE SEGURIDAD (Última semana) ===\n");
        report.append("Eventos por tipo:\n");
        
        for (Object[] stat : eventStats) {
            report.append("- ").append(stat[0]).append(": ").append(stat[1]).append("\n");
        }
        
        report.append("\nEventos críticos: ").append(criticalLogs.size()).append("\n");
        
        return report.toString();
    }

    /**
     * Limpiar logs antiguos (ejecutar periódicamente)
     */
    @Scheduled(fixedRate = 86400000) // Cada 24 horas
    public void cleanupOldLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(3); // Mantener por 3 meses
        securityLogRepository.deleteOldLogs(cutoff);
    }

    /**
     * Métodos de conveniencia para eventos comunes
     */
    public void logLoginSuccess(String userId, String ipAddress, String userAgent) {
        logSecurityEvent(userId, "LOGIN_SUCCESS", ipAddress, userAgent, "Login exitoso", "LOW");
    }

    public void logLoginFailure(String email, String ipAddress, String userAgent, String reason) {
        logSecurityEvent(null, "LOGIN_FAILED", ipAddress, userAgent, 
                        "Login fallido para " + email + ": " + reason, "MEDIUM");
    }

    public void logPasswordChange(String userId, String ipAddress, String userAgent) {
        logSecurityEvent(userId, "PASSWORD_CHANGED", ipAddress, userAgent, 
                        "Contraseña cambiada", "MEDIUM");
    }

    public void logAccountLocked(String userId, String ipAddress, String userAgent, String reason) {
        logSecurityEvent(userId, "ACCOUNT_LOCKED", ipAddress, userAgent, 
                        "Cuenta bloqueada: " + reason, "HIGH");
    }

    public void logSuspiciousActivity(String userId, String ipAddress, String userAgent, String details) {
        logSecurityEvent(userId, "SUSPICIOUS_ACTIVITY", ipAddress, userAgent, 
                        "Actividad sospechosa detectada: " + details, "CRITICAL");
    }
}
