package tech.nocountry.onboarding.modules.applications.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tech.nocountry.onboarding.services.notifications.NotificationPublisher;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(NotificationPublisher.class)
public class NotificationEventListener {

    private final NotificationPublisher notificationPublisher;

    @EventListener
    public void onStatusChanged(ApplicationStatusChangedEvent event) {
        notificationPublisher.notifyStatusChange(
                event.getUserId(),
                event.getApplicationId(),
                event.getPreviousStatus(),
                event.getNewStatus()
        );
    }
}
