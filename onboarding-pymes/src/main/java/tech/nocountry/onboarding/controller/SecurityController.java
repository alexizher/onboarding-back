package tech.nocountry.onboarding.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tech.nocountry.onboarding.services.AuthService;
import tech.nocountry.onboarding.services.SecurityAuditService;
import tech.nocountry.onboarding.services.PasswordValidationService;
import tech.nocountry.onboarding.security.JwtService;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            response.put("message", "Token de recuperación generado. Revisa tu email.");
            response.put("token", token); // En producción, esto se enviaría por email
        } else {
            response.put("success", false);
            response.put("message", "No se pudo generar el token. Verifica tu email o intenta más tarde.");
        }
        
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
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request, 
                                          @RequestHeader("Authorization") String authHeader,
                                          HttpServletRequest httpRequest) {
        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");
        
        if (currentPassword == null || newPassword == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Contraseña actual y nueva contraseña son requeridas");
            return ResponseEntity.badRequest().body(response);
        }

        // Extraer userId del token
        String userId = extractUserIdFromToken(authHeader);
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
     * Obtener IP del cliente
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
