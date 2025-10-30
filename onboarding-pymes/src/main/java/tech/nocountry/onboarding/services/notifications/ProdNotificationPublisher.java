package tech.nocountry.onboarding.services.notifications;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import tech.nocountry.onboarding.repositories.UserRepository;

@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProdNotificationPublisher implements NotificationPublisher {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Override
    public void notifyStatusChange(String userId, String applicationId, String previousStatus, String newStatus) {
        try {
            String recipient = userRepository.findByUserId(userId)
                    .map(u -> u.getEmail())
                    .orElse("no-reply@tu-dominio");

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipient);
            message.setSubject("Cambio de estado de solicitud " + applicationId);
            message.setText(String.format("La solicitud %s cambió de %s a %s", applicationId, previousStatus, newStatus));
            mailSender.send(message);
            log.info("[PROD NOTIFY] Email sent to {} for app={}, {} -> {}", recipient, applicationId, previousStatus, newStatus);
        } catch (Exception e) {
            log.error("[PROD NOTIFY] Error sending email notification: {}", e.getMessage());
        }
    }
}
