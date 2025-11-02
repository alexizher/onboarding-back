package tech.nocountry.onboarding.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tech.nocountry.onboarding.services.AuthService;
import tech.nocountry.onboarding.services.SecurityAuditService;
import tech.nocountry.onboarding.services.PasswordValidationService;
import tech.nocountry.onboarding.services.BlacklistService;
import tech.nocountry.onboarding.security.JwtService;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/security")
@CrossOrigin(origins = "*")
public class SecurityController {

    @Autowired
    private AuthService authService;

    @Autowired
    private SecurityAuditService securityAuditService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordValidationService passwordValidationService;

    @Autowired
    private BlacklistService blacklistService;

    @Autowired
    private tech.nocountry.onboarding.services.RateLimitingService rateLimitingService;

    @Autowired
    private tech.nocountry.onboarding.repositories.TokenBlacklistRepository tokenBlacklistRepository;

    @Autowired
    private tech.nocountry.onboarding.repositories.UserRepository userRepository;

    @Autowired
    private tech.nocountry.onboarding.services.AccountLockoutService accountLockoutService;


    /**
     * Generar token de recuperación de contraseña
     */
    @PostMapping("/password-reset/request")
    public ResponseEntity<?> requestPasswordReset(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String email = request.get("email");
        
        if (email == null || email.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Email es requerido");
            return ResponseEntity.badRequest().body(response);
        }

        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        String token = authService.generatePasswordResetToken(email, ipAddress, userAgent);
        
        Map<String, Object> response = new HashMap<>();
        if (token != null) {
            response.put("success", true);
            response.put("message", "Si el email existe, recibirás un enlace de recuperación. Por favor, verifica tu bandeja de entrada.");
            response.put("token", token); // En producción, esto se enviaría por email y no se incluiría en la respuesta
        } else {
            // No revelar si el email existe o no por seguridad
            response.put("success", false);
            response.put("message", "Si el email existe, recibirás un enlace de recuperación. Por favor, verifica tu bandeja de entrada.");
        }
        
        // Siempre devolver 200 para no revelar información sobre la existencia del email
        return ResponseEntity.ok(response);
    }

    /**
     * Cambiar contraseña usando token
     */
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<?> confirmPasswordReset(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");
        
        if (token == null || newPassword == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Token y nueva contraseña son requeridos");
            return ResponseEntity.badRequest().body(response);
        }

        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        boolean success = authService.resetPasswordWithToken(token, newPassword, ipAddress, userAgent);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "Contraseña cambiada exitosamente" : "Token inválido o expirado");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Cambiar contraseña de usuario autenticado
     */
    @PostMapping("/change-password")
    @PreAuthorize("hasAnyAuthority('ROLE_APPLICANT', 'ROLE_ANALYST', 'ROLE_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request, 
                                          Authentication authentication,
                                          HttpServletRequest httpRequest) {
        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");
        
        if (currentPassword == null || newPassword == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Contraseña actual y nueva contraseña son requeridas");
            return ResponseEntity.badRequest().body(response);
        }

        // Extraer userId del SecurityContext (igual que UserManagementController)
        String userId = getCurrentUserIdFromSecurityContext(authentication);
        if (userId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Token inválido o expirado");
            return ResponseEntity.badRequest().body(response);
        }
        
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        boolean success = authService.changePassword(userId, currentPassword, newPassword, ipAddress, userAgent);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "Contraseña cambiada exitosamente" : "Contraseña actual incorrecta");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Cerrar sesión
     */
    @PostMapping("/logout")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        
        boolean success = authService.logout(token);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "Sesión cerrada exitosamente" : "Error al cerrar sesión");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Obtener sesiones activas del usuario
     */
    @GetMapping("/sessions")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> getUserSessions(@RequestHeader("Authorization") String authHeader) {
        String userId = extractUserIdFromToken(authHeader);
        if (userId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Token inválido o expirado");
            return ResponseEntity.badRequest().body(response);
        }
        
        List<tech.nocountry.onboarding.entities.UserSession> sessions = authService.getUserActiveSessions(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("sessions", sessions);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Obtener logs de seguridad del usuario
     */
    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> getUserSecurityLogs(@RequestHeader("Authorization") String authHeader) {
        String userId = extractUserIdFromToken(authHeader);
        if (userId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Token inválido o expirado");
            return ResponseEntity.badRequest().body(response);
        }
        
        List<tech.nocountry.onboarding.entities.SecurityLog> logs = securityAuditService.getUserLogs(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("logs", logs);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Obtener reporte de seguridad (solo para administradores)
     */
    @GetMapping("/report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getSecurityReport() {
        String report = securityAuditService.generateSecurityReport();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("report", report);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Validar fortaleza de contraseña
     */
    @PostMapping("/validate-password")
    public ResponseEntity<?> validatePassword(@RequestBody Map<String, String> request) {
        String password = request.get("password");
        
        if (password == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Contraseña es requerida");
            return ResponseEntity.badRequest().body(response);
        }

        PasswordValidationService.PasswordValidationResult validation = 
            passwordValidationService.validatePassword(password);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("valid", validation.isValid());
        response.put("strength", validation.getStrength());
        response.put("strengthLevel", validation.getStrengthLevel());
        response.put("errors", validation.getErrors());
        response.put("warnings", validation.getWarnings());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Extraer userId del token JWT
     */
    private String extractUserIdFromToken(String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            return jwtService.extractUserId(token);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Invalidar una sesión específica
     */
    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> invalidateSession(@PathVariable String sessionId,
                                               @RequestHeader("Authorization") String authHeader,
                                               HttpServletRequest httpRequest) {
        String currentUserId = extractUserIdFromToken(authHeader);
        if (currentUserId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Token inválido o expirado");
            return ResponseEntity.badRequest().body(response);
        }

        // Verificar que la sesión pertenece al usuario (o es ADMIN)
        try {
            boolean success = authService.invalidateSession(currentUserId, sessionId);
            
            if (!success) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Sesión no encontrada o no pertenece al usuario");
                return ResponseEntity.badRequest().body(response);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Sesión invalidada exitosamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al invalidar sesión: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Cerrar todas las demás sesiones (excepto la actual)
     */
    @PostMapping("/sessions/close-others")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> closeOtherSessions(@RequestHeader("Authorization") String authHeader,
                                                HttpServletRequest httpRequest) {
        String userId = extractUserIdFromToken(authHeader);
        if (userId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Token inválido o expirado");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            String currentToken = authHeader.replace("Bearer ", "");
            
            // Cerrar todas las demás sesiones excepto la actual
            int closedCount = authService.closeOtherSessions(userId, currentToken);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Otras sesiones cerradas exitosamente");
            response.put("closedSessions", closedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al cerrar otras sesiones: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtener intentos de login recientes del usuario actual
     */
    @GetMapping("/login-attempts")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> getLoginAttempts(@RequestHeader("Authorization") String authHeader,
                                              @RequestParam(required = false) String email,
                                              HttpServletRequest httpRequest) {
        String userId = extractUserIdFromToken(authHeader);
        if (userId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Token inválido o expirado");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // Obtener email del usuario si no se proporciona
            if (email == null) {
                Optional<tech.nocountry.onboarding.entities.User> userOptional = userRepository.findById(userId);
                if (userOptional.isPresent()) {
                    email = userOptional.get().getEmail();
                }
            }
            
            List<tech.nocountry.onboarding.entities.LoginAttempt> attempts = 
                email != null ? rateLimitingService.getRecentAttemptsByEmail(email) : 
                rateLimitingService.getRecentAttemptsByIp(getClientIpAddress(httpRequest));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("attempts", attempts);
            response.put("count", attempts.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al obtener intentos de login: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Bloquear un usuario (solo ADMIN, MANAGER, ANALYST)
     */
    @PostMapping("/blacklist")
    @PreAuthorize("hasAnyRole('ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> blacklistUser(@RequestBody Map<String, String> request,
                                          @RequestHeader("Authorization") String authHeader,
                                          HttpServletRequest httpRequest) {
        String blacklistedBy = extractUserIdFromToken(authHeader);
        if (blacklistedBy == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Token inválido o expirado");
            return ResponseEntity.badRequest().body(response);
        }

        String userId = request.get("userId");
        String applicationId = request.get("applicationId"); // Opcional
        String reason = request.get("reason");
        
        if (userId == null || reason == null || reason.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "userId y reason son requeridos");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            
            tech.nocountry.onboarding.entities.ClientBlacklist blacklist = 
                blacklistService.blacklistUser(userId, applicationId, reason, blacklistedBy, ipAddress, userAgent);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Usuario bloqueado exitosamente");
            response.put("blacklist", blacklist);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al bloquear usuario: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Desbloquear un usuario (solo ADMIN, MANAGER)
     */
    @PostMapping("/blacklist/{userId}/unblacklist")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<?> unblacklistUser(@PathVariable String userId,
                                            @RequestBody Map<String, String> request,
                                            @RequestHeader("Authorization") String authHeader,
                                            HttpServletRequest httpRequest) {
        String unblacklistedBy = extractUserIdFromToken(authHeader);
        if (unblacklistedBy == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Token inválido o expirado");
            return ResponseEntity.badRequest().body(response);
        }

        String reason = request.get("reason");
        if (reason == null || reason.trim().isEmpty()) {
            reason = "Desbloqueo manual";
        }

        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            
            boolean success = blacklistService.unblacklistUser(userId, unblacklistedBy, reason, ipAddress, userAgent);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "Usuario desbloqueado exitosamente" : "Usuario no está bloqueado");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al desbloquear usuario: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Verificar si un usuario está bloqueado
     */
    @GetMapping("/blacklist/{userId}")
    @PreAuthorize("hasAnyRole('ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> checkUserBlacklist(@PathVariable String userId) {
        try {
            boolean isBlacklisted = blacklistService.isUserBlacklisted(userId);
            List<tech.nocountry.onboarding.entities.ClientBlacklist> blacklists = 
                blacklistService.getUserActiveBlacklists(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("isBlacklisted", isBlacklisted);
            response.put("blacklists", blacklists);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al verificar blacklist: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtener historial de bloqueos de un usuario
     */
    @GetMapping("/blacklist/{userId}/history")
    @PreAuthorize("hasAnyRole('ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> getUserBlacklistHistory(@PathVariable String userId) {
        try {
            List<tech.nocountry.onboarding.entities.ClientBlacklist> history = 
                blacklistService.getUserBlacklistHistory(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("history", history);
            response.put("count", history.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al obtener historial: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtener tokens blacklisted del usuario actual
     */
    @GetMapping("/tokens/blacklisted")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> getUserBlacklistedTokens(@RequestHeader("Authorization") String authHeader) {
        String userId = extractUserIdFromToken(authHeader);
        if (userId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Token inválido o expirado");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            List<tech.nocountry.onboarding.entities.TokenBlacklist> blacklistedTokens = 
                tokenBlacklistRepository.findByUserIdOrderByBlacklistedAtDesc(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("tokens", blacklistedTokens);
            response.put("count", blacklistedTokens.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al obtener tokens blacklisted: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Desbloquear cuenta de usuario (solo ADMIN, MANAGER)
     */
    @PostMapping("/accounts/{userId}/unlock")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<?> unlockAccount(@PathVariable String userId,
                                         @RequestBody Map<String, String> request,
                                         @RequestHeader("Authorization") String authHeader,
                                         HttpServletRequest httpRequest) {
        String unlockedBy = extractUserIdFromToken(authHeader);
        if (unlockedBy == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Token inválido o expirado");
            return ResponseEntity.badRequest().body(response);
        }

        String reason = request.get("reason");
        if (reason == null || reason.trim().isEmpty()) {
            reason = "Desbloqueo manual por administrador";
        }

        try {
            Optional<tech.nocountry.onboarding.entities.User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Usuario no encontrado");
                return ResponseEntity.badRequest().body(response);
            }

            tech.nocountry.onboarding.entities.User user = userOptional.get();
            
            // Usar AccountLockoutService para desbloquear
            accountLockoutService.unlockAccount(user, unlockedBy, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cuenta desbloqueada exitosamente");
            response.put("userId", userId);
            response.put("unlockedBy", unlockedBy);
            response.put("reason", reason);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al desbloquear cuenta: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Verificar estado de bloqueo de una cuenta (solo ADMIN, MANAGER, ANALYST)
     */
    @GetMapping("/accounts/{userId}/lockout-status")
    @PreAuthorize("hasAnyRole('ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> getAccountLockoutStatus(@PathVariable String userId) {
        try {
            Optional<tech.nocountry.onboarding.entities.User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Usuario no encontrado");
                return ResponseEntity.badRequest().body(response);
            }

            tech.nocountry.onboarding.entities.User user = userOptional.get();
            
            boolean isLocked = accountLockoutService.isAccountLocked(user);
            long remainingMinutes = accountLockoutService.getRemainingLockoutMinutes(user);
            String lockoutMessage = accountLockoutService.getLockoutMessage(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userId", userId);
            response.put("isLocked", isLocked);
            response.put("failedLoginAttempts", user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0);
            response.put("lockoutLevel", user.getLockoutLevel() != null ? user.getLockoutLevel() : 0);
            response.put("lockoutUntil", user.getLockoutUntil());
            response.put("remainingMinutes", remainingMinutes);
            response.put("lockoutMessage", lockoutMessage);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al obtener estado de bloqueo: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtener IP del cliente
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Obtener userId del SecurityContext (igual que UserManagementController)
     */
    private String getCurrentUserIdFromSecurityContext(Authentication authentication) {
        try {
            // Primero intentar con el parámetro authentication
            if (authentication != null) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof tech.nocountry.onboarding.entities.User) {
                    tech.nocountry.onboarding.entities.User user = (tech.nocountry.onboarding.entities.User) principal;
                    return user.getUserId();
                }
            }
            
            // Fallback: usar SecurityContextHolder (igual que UserManagementController)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                Object principal = auth.getPrincipal();
                if (principal instanceof tech.nocountry.onboarding.entities.User) {
                    return ((tech.nocountry.onboarding.entities.User) principal).getUserId();
                }
                
                // Si no es User, intentar obtener desde el nombre de usuario (como UserManagementController)
                String username = auth.getName();
                if (username != null) {
                    // Intentar buscar el usuario por username para obtener userId
                    Optional<tech.nocountry.onboarding.entities.User> userOptional = userRepository.findByUsername(username);
                    if (userOptional.isPresent()) {
                        return userOptional.get().getUserId();
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
