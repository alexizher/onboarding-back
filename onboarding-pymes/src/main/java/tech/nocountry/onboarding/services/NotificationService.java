package tech.nocountry.onboarding.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.nocountry.onboarding.entities.EmailNotification;
import tech.nocountry.onboarding.entities.SmsNotification;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.repositories.EmailNotificationRepository;
import tech.nocountry.onboarding.repositories.SmsNotificationRepository;
import tech.nocountry.onboarding.repositories.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de notificaciones Email/SMS
 * En modo desarrollo/testing: Guarda notificaciones en BD sin enviarlas realmente
 * En producción: Puede integrarse con servicios externos (JavaMailSender, Twilio, etc.)
 */
@Slf4j
@Service
@Transactional
public class NotificationService {

    @Autowired
    private EmailNotificationRepository emailNotificationRepository;

    @Autowired
    private SmsNotificationRepository smsNotificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${notification.mock.enabled:true}")
    private boolean mockEnabled;

    @Value("${notification.max-retries:3}")
    private int maxRetries;

    /**
     * Enviar notificación por email
     */
    public void sendEmail(String userId, String subject, String body, String notificationType) {
        try {
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                log.warn("NotificationService: Usuario no encontrado para notificación email: {}", userId);
                return;
            }

            User user = userOptional.get();
            String email = user.getEmail();

            EmailNotification notification = EmailNotification.builder()
                    .notificationId(java.util.UUID.randomUUID().toString())
                    .userId(userId)
                    .email(email)
                    .subject(subject)
                    .body(body)
                    .notificationType(notificationType)
                    .createdAt(LocalDateTime.now())
                    .isSent(false)
                    .isRead(false)
                    .retryCount(0)
                    .build();

            if (mockEnabled) {
                // Modo mock: Guardar en BD pero marcar como "enviado" (simulado)
                notification.setIsSent(true);
                notification.setSentAt(LocalDateTime.now());
                log.info("NotificationService [MOCK]: Email notificación guardada (no enviada realmente) - Usuario: {}, Tipo: {}, Asunto: {}", 
                        userId, notificationType, subject);
            } else {
                // Modo producción: Aquí se integraría con JavaMailSender o servicio externo
                // Por ahora, solo guardar como pendiente
                log.warn("NotificationService: Modo producción no implementado aún. Notificación guardada como pendiente.");
            }

            emailNotificationRepository.save(notification);

        } catch (Exception e) {
            log.error("NotificationService: Error al guardar notificación email: {}", e.getMessage(), e);
        }
    }

    /**
     * Enviar notificación por SMS
     */
    public void sendSms(String userId, String message, String notificationType) {
        try {
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                log.warn("NotificationService: Usuario no encontrado para notificación SMS: {}", userId);
                return;
            }

            User user = userOptional.get();
            String phone = user.getPhone();

            if (phone == null || phone.isEmpty()) {
                log.warn("NotificationService: Usuario no tiene teléfono registrado: {}", userId);
                return;
            }

            SmsNotification notification = SmsNotification.builder()
                    .notificationId(java.util.UUID.randomUUID().toString())
                    .userId(userId)
                    .phone(phone)
                    .message(message)
                    .notificationType(notificationType)
                    .createdAt(LocalDateTime.now())
                    .isSent(false)
                    .isRead(false)
                    .retryCount(0)
                    .build();

            if (mockEnabled) {
                // Modo mock: Guardar en BD pero marcar como "enviado" (simulado)
                notification.setIsSent(true);
                notification.setSentAt(LocalDateTime.now());
                log.info("NotificationService [MOCK]: SMS notificación guardada (no enviada realmente) - Usuario: {}, Tipo: {}, Mensaje: {}", 
                        userId, notificationType, message);
            } else {
                // Modo producción: Aquí se integraría con Twilio, AWS SNS, etc.
                // Por ahora, solo guardar como pendiente
                log.warn("NotificationService: Modo producción no implementado aún. Notificación guardada como pendiente.");
            }

            smsNotificationRepository.save(notification);

        } catch (Exception e) {
            log.error("NotificationService: Error al guardar notificación SMS: {}", e.getMessage(), e);
        }
    }

    /**
     * Notificar registro de nueva cuenta
     */
    public void notifyRegistration(String userId) {
        if (!emailEnabled) return;

        sendEmail(userId, 
                "Bienvenido - Registro Exitoso", 
                "Tu cuenta ha sido registrada exitosamente. Por favor, verifica tu email.",
                "REGISTRATION");
    }

    /**
     * Notificar cambio de contraseña
     */
    public void notifyPasswordChange(String userId, String ipAddress) {
        if (!emailEnabled) return;

        sendEmail(userId,
                "Cambio de Contraseña",
                String.format("Tu contraseña ha sido cambiada exitosamente. Si no realizaste este cambio, por favor contacta a soporte.\n\nIP: %s\nFecha: %s", 
                        ipAddress, LocalDateTime.now()),
                "PASSWORD_CHANGE");
    }

    /**
     * Notificar login desde dispositivo/ubicación nueva
     */
    public void notifyNewLogin(String userId, String ipAddress, String userAgent) {
        if (!emailEnabled) return;

        sendEmail(userId,
                "Nuevo Inicio de Sesión Detectado",
                String.format("Se ha iniciado sesión en tu cuenta desde una nueva ubicación/dispositivo.\n\nIP: %s\nUser Agent: %s\nFecha: %s\n\nSi no fuiste tú, cambia tu contraseña inmediatamente.",
                        ipAddress, userAgent, LocalDateTime.now()),
                "NEW_LOGIN");
    }

    /**
     * Notificar bloqueo de cuenta
     */
    public void notifyAccountLocked(String userId, String reason) {
        if (!emailEnabled) return;

        sendEmail(userId,
                "Cuenta Bloqueada por Seguridad",
                String.format("Tu cuenta ha sido bloqueada temporalmente por seguridad.\n\nRazón: %s\n\nLa cuenta se desbloqueará automáticamente después del período de bloqueo.", reason),
                "ACCOUNT_LOCKED");
    }

    /**
     * Notificar actividad sospechosa
     */
    public void notifySuspiciousActivity(String userId, String activity, String ipAddress) {
        if (!emailEnabled) return;

        sendEmail(userId,
                "Actividad Sospechosa Detectada",
                String.format("Se ha detectado actividad sospechosa en tu cuenta.\n\nActividad: %s\nIP: %s\nFecha: %s\n\nPor favor, revisa tu cuenta y cambia tu contraseña si es necesario.",
                        activity, ipAddress, LocalDateTime.now()),
                "SUSPICIOUS_ACTIVITY");
    }

    /**
     * Notificar verificación de email
     */
    public void notifyEmailVerification(String userId, String verificationToken) {
        if (!emailEnabled) return;

        sendEmail(userId,
                "Verifica tu Email",
                String.format("Por favor, verifica tu email haciendo clic en el siguiente enlace:\n\nhttp://localhost:8080/api/auth/verify-email?token=%s\n\nEste enlace expira en 1 hora.",
                        verificationToken),
                "EMAIL_VERIFICATION");
    }

    /**
     * Notificar recuperación de contraseña
     */
    public void notifyPasswordReset(String userId, String resetToken) {
        if (!emailEnabled) return;

        sendEmail(userId,
                "Recuperación de Contraseña",
                String.format("Has solicitado recuperar tu contraseña. Usa el siguiente enlace:\n\nhttp://localhost:8080/api/security/password-reset/confirm?token=%s\n\nEste enlace expira en 1 hora. Si no solicitaste esto, ignora este email.",
                        resetToken),
                "PASSWORD_RESET");
    }

    /**
     * Obtener notificaciones de email de un usuario
     */
    @Transactional(readOnly = true)
    public List<EmailNotification> getUserEmailNotifications(String userId) {
        return emailNotificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Obtener notificaciones de SMS de un usuario
     */
    @Transactional(readOnly = true)
    public List<SmsNotification> getUserSmsNotifications(String userId) {
        return smsNotificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Obtener notificaciones no leídas
     */
    @Transactional(readOnly = true)
    public List<EmailNotification> getUnreadEmailNotifications(String userId) {
        return emailNotificationRepository.findUnreadByUserId(userId);
    }

    /**
     * Marcar notificación como leída
     */
    public void markEmailNotificationAsRead(String notificationId, String userId) {
        Optional<EmailNotification> notificationOptional = emailNotificationRepository.findById(notificationId);
        if (notificationOptional.isPresent()) {
            EmailNotification notification = notificationOptional.get();
            if (notification.getUserId().equals(userId)) {
                notification.setIsRead(true);
                notification.setReadAt(LocalDateTime.now());
                emailNotificationRepository.save(notification);
            }
        }
    }

    /**
     * Contar notificaciones no leídas
     */
    @Transactional(readOnly = true)
    public long countUnreadEmailNotifications(String userId) {
        return emailNotificationRepository.countUnreadByUserId(userId);
    }

    /**
     * Intentar reenviar notificaciones pendientes (para producción futura)
     */
    @Scheduled(fixedRate = 3600000) // Cada hora
    public void retryPendingNotifications() {
        if (mockEnabled) {
            // En modo mock, no hay nada que reenviar
            return;
        }

        List<EmailNotification> pendingEmails = emailNotificationRepository.findPendingNotifications(maxRetries);
        for (EmailNotification notification : pendingEmails) {
            try {
                // Aquí se integraría con el servicio real de email
                // Por ahora, solo log
                log.info("NotificationService: Reintentando enviar notificación email: {}", notification.getNotificationId());
                notification.setRetryCount(notification.getRetryCount() + 1);
                emailNotificationRepository.save(notification);
            } catch (Exception e) {
                log.error("NotificationService: Error al reintentar notificación: {}", e.getMessage());
            }
        }
    }
}

