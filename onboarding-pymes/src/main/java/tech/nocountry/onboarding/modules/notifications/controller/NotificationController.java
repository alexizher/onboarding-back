package tech.nocountry.onboarding.modules.notifications.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.security.JwtService;
import tech.nocountry.onboarding.services.notifications.SseNotificationService;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final SseNotificationService sseNotificationService;
    private final JwtService jwtService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> stream(@RequestParam(required = false) String token) {
        log.info("SSE: Solicitud de stream recibida");
        
        try {
            String userId;
            
            // Si hay token en query param (para EventSource de Angular que no puede usar headers)
            if (token != null && !token.isBlank()) {
                log.debug("SSE: Token recibido por query parameter (modo desarrollo)");
                try {
                    // Validar token antes de extraer userId
                    if (!jwtService.validateToken(token)) {
                        log.error("SSE: Token inválido o expirado en query parameter");
                        throw new IllegalArgumentException("Token inválido o expirado");
                    }
                    userId = jwtService.extractUserId(token);
                    log.info("SSE: userId extraído desde query param: {}", userId);
                } catch (Exception e) {
                    log.error("SSE: Error al extraer/validar userId desde query param: {}", e.getMessage());
                    throw new IllegalArgumentException("Token inválido en query parameter: " + e.getMessage());
                }
            } else {
                // Modo normal: extraer desde SecurityContext
                userId = getCurrentUserId();
                log.info("SSE: userId extraído desde SecurityContext: {}", userId);
            }
            
            if (userId == null || userId.isBlank()) {
                log.error("SSE: userId es null o vacío después de la extracción");
                throw new IllegalStateException("userId no disponible");
            }
            
            log.info("SSE: Suscribiendo usuario {} al stream. Total usuarios conectados antes: {}", 
                     userId, sseNotificationService.getActiveUserCount());
            
            SseEmitter emitter = sseNotificationService.subscribe(userId);
            
            log.info("SSE: Usuario {} suscrito exitosamente. Total usuarios conectados después: {}", 
                     userId, sseNotificationService.getActiveUserCount());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setCacheControl("no-cache");
            headers.set("X-Accel-Buffering", "no"); // Nginx - deshabilita buffering
            headers.setConnection("keep-alive");
            headers.set("Content-Type", "text/event-stream");
            headers.set("Access-Control-Allow-Origin", "*"); // Para desarrollo - ajustar en producción
            headers.set("Access-Control-Allow-Credentials", "true");
            
            log.info("SSE: Stream creado exitosamente para usuario {}", userId);
            return ResponseEntity.ok().headers(headers).body(emitter);
            
        } catch (IllegalArgumentException e) {
            log.error("SSE: Error de validación al crear stream: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("SSE: Error inesperado al crear stream: {}", e.getMessage(), e);
            throw new RuntimeException("Error al crear SSE stream", e);
        }
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null) {
            log.error("SSE: Authentication es null en SecurityContext");
            throw new IllegalStateException("No hay autenticación activa");
        }
        
        Object principal = auth.getPrincipal();
        
        if (principal == null) {
            log.error("SSE: Principal es null en Authentication");
            throw new IllegalStateException("Principal de autenticación no disponible");
        }
        
        log.debug("SSE: Principal type: {}", principal.getClass().getName());
        
        if (principal instanceof User user) {
            String userId = user.getUserId();
            if (userId == null || userId.isBlank()) {
                log.error("SSE: userId es null o vacío en User object");
                throw new IllegalStateException("userId del usuario no disponible");
            }
            log.debug("SSE: userId extraído: {}", userId);
            return userId;
        }
        
        log.error("SSE: Principal no es instancia de User. Tipo: {}", principal.getClass().getName());
        throw new IllegalStateException("Principal no es una instancia de User");
    }
}


