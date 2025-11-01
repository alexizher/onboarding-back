package tech.nocountry.onboarding.modules.applications.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tech.nocountry.onboarding.services.notifications.NotificationPublisher;
import tech.nocountry.onboarding.services.notifications.SseNotificationService;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final Optional<NotificationPublisher> notificationPublisher;
    private final SseNotificationService sseNotificationService;

    @EventListener
    public void onStatusChanged(ApplicationStatusChangedEvent event) {
        log.info("NotificationEventListener: Evento de cambio de estado recibido - aplicación: {}, usuario: {}, {} -> {}", 
                 event.getApplicationId(), event.getUserId(), event.getPreviousStatus(), event.getNewStatus());
        
        try {
            notificationPublisher.ifPresent(np -> {
                try {
                    np.notifyStatusChange(
                            event.getUserId(),
                            event.getApplicationId(),
                            event.getPreviousStatus(),
                            event.getNewStatus()
                    );
                    log.debug("NotificationEventListener: Notificación publicada exitosamente");
                } catch (Exception ex) {
                    log.error("NotificationEventListener: Error al publicar notificación: {}", ex.getMessage(), ex);
                }
            });
        } catch (Exception ex) {
            log.error("NotificationEventListener: Error al acceder a NotificationPublisher: {}", ex.getMessage(), ex);
        }

        try {
            var payload = java.util.Map.of(
                    "type", "APPLICATION_STATUS_CHANGED",
                    "applicationId", event.getApplicationId(),
                    "oldStatus", event.getPreviousStatus(),
                    "newStatus", event.getNewStatus(),
                    "timestamp", java.time.Instant.now().toString()
            );
            
            log.info("NotificationEventListener: Enviando SSE a usuario {} para aplicación {}", 
                     event.getUserId(), event.getApplicationId());
            
            sseNotificationService.sendToUser(event.getUserId(), "application-status", payload);
            
            log.info("NotificationEventListener: SSE enviado exitosamente a usuario {}", event.getUserId());
        } catch (Exception ex) {
            log.error("NotificationEventListener: No se pudo enviar SSE para la aplicación {} al usuario {}: {}", 
                     event.getApplicationId(), event.getUserId(), ex.getMessage(), ex);
        }
    }
}
