package tech.nocountry.onboarding.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.nocountry.onboarding.entities.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
