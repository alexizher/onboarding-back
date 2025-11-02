package tech.nocountry.onboarding.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.nocountry.onboarding.entities.EmailNotification;
import tech.nocountry.onboarding.entities.SmsNotification;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.repositories.UserRepository;
import tech.nocountry.onboarding.services.NotificationService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller para gestión de notificaciones Email/SMS
 * Nota: Este controller maneja las notificaciones guardadas en BD
 * El controller existente en modules/notifications maneja SSE (Server-Sent Events)
 */
@RestController
@RequestMapping("/api/notifications-management")
public class NotificationManagementController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Obtener notificaciones de email del usuario actual
     */
    @GetMapping("/emails")
    @PreAuthorize("hasAnyAuthority('ROLE_APPLICANT', 'ROLE_ANALYST', 'ROLE_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<?> getMyEmailNotifications(Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        if (userId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Usuario no autenticado");
            return ResponseEntity.badRequest().body(response);
        }

        List<EmailNotification> notifications = notificationService.getUserEmailNotifications(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("notifications", notifications);
        response.put("count", notifications.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Obtener notificaciones de email no leídas del usuario actual
     */
    @GetMapping("/emails/unread")
    @PreAuthorize("hasAnyAuthority('ROLE_APPLICANT', 'ROLE_ANALYST', 'ROLE_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<?> getUnreadEmailNotifications(Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        if (userId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Usuario no autenticado");
            return ResponseEntity.badRequest().body(response);
        }

        List<EmailNotification> notifications = notificationService.getUnreadEmailNotifications(userId);
        long count = notificationService.countUnreadEmailNotifications(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("notifications", notifications);
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    /**
     * Marcar notificación de email como leída
     */
    @PutMapping("/emails/{notificationId}/read")
    @PreAuthorize("hasAnyAuthority('ROLE_APPLICANT', 'ROLE_ANALYST', 'ROLE_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<?> markEmailNotificationAsRead(@PathVariable String notificationId, 
                                                          Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        if (userId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Usuario no autenticado");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            notificationService.markEmailNotificationAsRead(notificationId, userId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notificación marcada como leída");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al marcar notificación: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Obtener contador de notificaciones no leídas
     */
    @GetMapping("/emails/unread/count")
    @PreAuthorize("hasAnyAuthority('ROLE_APPLICANT', 'ROLE_ANALYST', 'ROLE_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<?> getUnreadEmailCount(Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        if (userId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Usuario no autenticado");
            return ResponseEntity.badRequest().body(response);
        }

        long count = notificationService.countUnreadEmailNotifications(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("unreadCount", count);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtener notificaciones de SMS del usuario actual
     */
    @GetMapping("/sms")
    @PreAuthorize("hasAnyAuthority('ROLE_APPLICANT', 'ROLE_ANALYST', 'ROLE_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<?> getMySmsNotifications(Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        if (userId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Usuario no autenticado");
            return ResponseEntity.badRequest().body(response);
        }

        List<SmsNotification> notifications = notificationService.getUserSmsNotifications(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("notifications", notifications);
        response.put("count", notifications.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Obtener userId del contexto de seguridad
     */
    private String getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getUserId();
        }
        
        // Fallback: buscar por username
        if (authentication != null && authentication.getName() != null) {
            Optional<User> userOptional = userRepository.findByUsername(authentication.getName());
            if (userOptional.isPresent()) {
                return userOptional.get().getUserId();
            }
        }
        
        return null;
    }
}

