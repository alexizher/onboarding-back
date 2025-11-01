package tech.nocountry.onboarding.services.notifications;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class SseNotificationService {

    private static final long DEFAULT_TIMEOUT_MS = Duration.ofMinutes(30).toMillis();

    private final Map<String, List<SseEmitter>> userIdToEmitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String userId) {
        log.info("SSE: Usuario {} intentando suscribirse al stream", userId);
        
        if (userId == null || userId.isBlank()) {
            log.error("SSE: userId es null o vacío, rechazando suscripción");
            throw new IllegalArgumentException("userId no puede ser null o vacío");
        }

        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MS);

        emitter.onCompletion(() -> {
            log.info("SSE: Emitter completado para usuario {}", userId);
            removeEmitter(userId, emitter);
        });
        
        emitter.onTimeout(() -> {
            log.warn("SSE: Timeout del emitter para usuario {} ({} minutos)", userId, DEFAULT_TIMEOUT_MS / 60000);
            removeEmitter(userId, emitter);
        });
        
        emitter.onError((ex) -> {
            log.error("SSE: Error en emitter para usuario {}: {}", userId, ex.getMessage(), ex);
            removeEmitter(userId, emitter);
        });

        userIdToEmitters.computeIfAbsent(userId, k -> {
            log.debug("SSE: Creando nueva lista de emitters para usuario {}", userId);
            return new CopyOnWriteArrayList<>();
        }).add(emitter);

        log.info("SSE: Usuario {} suscrito exitosamente. Emitters activos: {}", 
                 userId, userIdToEmitters.get(userId).size());

        // Send initial event (handshake)
        try {
            emitter.send(SseEmitter.event().name("init").data("connected"));
            log.debug("SSE: Evento 'init' enviado a usuario {}", userId);
        } catch (IOException e) {
            log.error("SSE: Error al enviar evento 'init' a usuario {}: {}", userId, e.getMessage());
            removeEmitter(userId, emitter);
            throw new RuntimeException("Error al inicializar SSE stream", e);
        }

        return emitter;
    }

    public void sendToUser(String userId, String eventName, Object payload) {
        log.info("SSE: Intentando enviar evento '{}' a usuario {}", eventName, userId);
        log.info("SSE: Usuarios con emitters activos: {}", userIdToEmitters.keySet());
        
        List<SseEmitter> emitters = userIdToEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            log.warn("SSE: No hay emitters activos para usuario {} (evento '{}' no enviado)", userId, eventName);
            log.warn("SSE: Emitters disponibles para otros usuarios: {}", 
                     userIdToEmitters.entrySet().stream()
                             .filter(e -> !e.getValue().isEmpty())
                             .map(e -> String.format("%s (%d emitters)", e.getKey(), e.getValue().size()))
                             .toList());
            return;
        }

        log.info("SSE: Enviando evento '{}' a usuario {} ({} emitters activos)", eventName, userId, emitters.size());
        
        int successCount = 0;
        int errorCount = 0;
        
        for (SseEmitter emitter : List.copyOf(emitters)) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
                successCount++;
                log.debug("SSE: Evento '{}' enviado exitosamente a usuario {}", eventName, userId);
            } catch (IOException ex) {
                errorCount++;
                log.error("SSE: Error al enviar evento '{}' a usuario {}: {}", eventName, userId, ex.getMessage());
                removeEmitter(userId, emitter);
            }
        }
        
        log.info("SSE: Evento '{}' procesado para usuario {}. Exitosos: {}, Errores: {}", 
                 eventName, userId, successCount, errorCount);
    }

    // Heartbeat para mantener conexiones activas y limpiar las caídas
    @Scheduled(fixedDelay = 10000)
    public void heartbeat() {
        int totalUsers = userIdToEmitters.size();
        int totalEmitters = userIdToEmitters.values().stream()
                .mapToInt(List::size)
                .sum();
        
        if (totalUsers == 0) {
            return;
        }
        
        log.debug("SSE: Heartbeat - {} usuarios activos, {} emitters totales", totalUsers, totalEmitters);
        
        for (Map.Entry<String, List<SseEmitter>> entry : userIdToEmitters.entrySet()) {
            String userId = entry.getKey();
            for (SseEmitter emitter : List.copyOf(entry.getValue())) {
                try {
                    emitter.send(SseEmitter.event().name("ping").data("keep-alive"));
                } catch (IOException ex) {
                    log.debug("SSE: Emitter caído durante heartbeat para usuario {}, removiendo", userId);
                    removeEmitter(userId, emitter);
                }
            }
        }
    }

    private void removeEmitter(String userId, SseEmitter emitter) {
        List<SseEmitter> emitters = userIdToEmitters.get(userId);
        if (emitters == null) return;
        
        boolean removed = emitters.remove(emitter);
        if (removed) {
            log.debug("SSE: Emitter removido para usuario {}. Emitters restantes: {}", 
                     userId, emitters.size());
        }
        
        if (emitters.isEmpty()) {
            userIdToEmitters.remove(userId);
            log.info("SSE: Usuario {} desconectado completamente (sin emitters activos)", userId);
        }
    }

    // Método para obtener estadísticas de conexiones activas (para debugging)
    public int getActiveUserCount() {
        return userIdToEmitters.size();
    }

    public int getTotalEmitterCount() {
        return userIdToEmitters.values().stream()
                .mapToInt(List::size)
                .sum();
    }
}


