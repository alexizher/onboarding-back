package tech.nocountry.onboarding.modules.documents.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tech.nocountry.onboarding.services.notifications.SseNotificationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentVerificationEventListener {

    private final SseNotificationService sseNotificationService;

    @EventListener
    public void onDocumentVerified(DocumentVerifiedEvent event) {
        log.info("DocumentVerificationEventListener: Evento de verificación recibido - documento: {}, usuario: {}, estado: {}", 
                 event.getDocumentId(), event.getUserId(), event.getVerificationStatus());
        
        try {
            var payload = java.util.Map.of(
                    "type", "DOCUMENT_VERIFIED",
                    "documentId", event.getDocumentId(),
                    "applicationId", event.getApplicationId(),
                    "verificationStatus", event.getVerificationStatus(),
                    "verifiedByUserId", event.getVerifiedByUserId(),
                    "timestamp", java.time.Instant.now().toString()
            );
            
            log.info("DocumentVerificationEventListener: Enviando SSE a usuario {} para documento {}", 
                     event.getUserId(), event.getDocumentId());
            
            sseNotificationService.sendToUser(event.getUserId(), "document-verified", payload);
            
            log.info("DocumentVerificationEventListener: SSE enviado exitosamente a usuario {}", event.getUserId());
        } catch (Exception ex) {
            log.error("DocumentVerificationEventListener: No se pudo enviar SSE para el documento {} al usuario {}: {}", 
                     event.getDocumentId(), event.getUserId(), ex.getMessage(), ex);
        }
    }
}

