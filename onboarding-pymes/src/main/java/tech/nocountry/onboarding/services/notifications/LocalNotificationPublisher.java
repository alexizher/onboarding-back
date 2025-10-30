package tech.nocountry.onboarding.services.notifications;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalNotificationPublisher implements NotificationPublisher {

    @Override
    public void notifyStatusChange(String userId, String applicationId, String previousStatus, String newStatus) {
        log.info("[LOCAL NOTIFY] user={}, app={}, {} -> {}", userId, applicationId, previousStatus, newStatus);
    }
}
