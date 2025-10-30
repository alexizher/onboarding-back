package tech.nocountry.onboarding.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.nocountry.onboarding.entities.AuditLog;
import tech.nocountry.onboarding.repositories.AuditLogRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void record(String userId, String action, String ip, String description) {
        AuditLog log = AuditLog.builder()
                .userId(userId)
                .action(action)
                .ip(ip)
                .description(description)
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(log);
    }
}


