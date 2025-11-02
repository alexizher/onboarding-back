package tech.nocountry.onboarding.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import tech.nocountry.onboarding.services.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Endpoint para probar la conectividad
     */
    @GetMapping("/test")
    public String test() {
        return "Auth Controller funcionando correctamente!";
    }

    /**
     * Endpoint para probar BCrypt directamente
     */
    @PostMapping("/test-bcrypt")
    public Map<String, Object> testBcrypt(@RequestBody Map<String, String> request) {
        String password = request.get("password");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Test básico de BCrypt
            String encoded = authService.passwordEncoder.encode(password);
            boolean matches = authService.passwordEncoder.matches(password, encoded);
            
            response.put("success", true);
            response.put("passwordLength", password.length());
            response.put("passwordBytes", password.getBytes().length);
            response.put("encodedLength", encoded.length());
            response.put("matches", matches);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return response;
    }

    /**
     * Endpoint para probar el login con debug
     */
    @PostMapping("/test-login")
    public Map<String, Object> testLogin(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");
        
        System.out.println("TEST LOGIN: Email: " + email);
        System.out.println("TEST LOGIN: Password: " + password);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Test login received");
        response.put("email", email);
        response.put("passwordLength", password != null ? password.length() : 0);
        
        return response;
    }

    /**
     * Registro de nuevos usuarios
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> request) {
        String dni = request.get("dni");
        String fullName = request.get("fullName");
        String username = request.get("username");
        String email = request.get("email");
        String password = request.get("password");
        String phone = request.get("phone");
        
        
        // Validaciones básicas
        if (username == null || email == null || password == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Username, email y password son requeridos");
            return response;
        }
        
        return authService.registerUser(username, email, password, fullName, phone, dni);
    }

    /**
     * Login de usuarios
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        System.out.println("DEBUG CONTROLLER: Login request received");
        String email = request.get("email");
        String password = request.get("password");
        
        System.out.println("DEBUG CONTROLLER: Email: " + email);
        System.out.println("DEBUG CONTROLLER: Password length: " + (password != null ? password.length() : 0));
        
        if (email == null || password == null) {
            System.out.println("DEBUG CONTROLLER: Missing email or password");
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Email y password son requeridos");
            return response;
        }

        // Obtener información de la solicitud
        String userAgent = httpRequest.getHeader("User-Agent");
        String clientIp = getClientIpAddress(httpRequest);
        System.out.println("DEBUG CONTROLLER: Client IP: " + clientIp);
        System.out.println("DEBUG CONTROLLER: User Agent: " + userAgent);

        // Verificar si se requiere CAPTCHA
        boolean captchaRequired = captchaService.isCaptchaRequired(email, clientIp);
        String captchaToken = request.get("captchaToken");
        
        if (captchaRequired && (captchaToken == null || captchaToken.isEmpty())) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Se requiere verificación CAPTCHA debido a múltiples intentos fallidos");
            response.put("captchaRequired", true);
            response.put("remainingAttempts", Math.max(0, 3 - captchaService.getRequiredAfterAttempts()));
            return response;
        }
        
        // TODO: Validar CAPTCHA token con servicio externo (Google reCAPTCHA, hCaptcha, etc.)
        // Por ahora solo verificamos que se proporcione si es requerido

        // Crear LoginRequest
        tech.nocountry.onboarding.dto.LoginRequest loginRequest = new tech.nocountry.onboarding.dto.LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        // Usar directamente el método simple para evitar problemas con servicios de seguridad
        tech.nocountry.onboarding.dto.AuthResponse authResponse;
        try {
            authResponse = authService.loginUserSimple(loginRequest);
        } catch (Exception e) {
            System.out.println("DEBUG: Error in simple login: " + e.getMessage());
            authResponse = tech.nocountry.onboarding.dto.AuthResponse.error("Error en el login: " + e.getMessage());
        }
        
        // Convertir AuthResponse a Map para compatibilidad
        Map<String, Object> response = new HashMap<>();
        response.put("success", authResponse.isSuccess());
        response.put("message", authResponse.getMessage());
        
        if (authResponse.isSuccess()) {
            response.put("userId", authResponse.getUserId());
            response.put("email", authResponse.getEmail());
            response.put("username", authResponse.getUsername());
            response.put("fullName", authResponse.getFullName());
            response.put("token", authResponse.getToken());
        }
        
        return response;
    }

    /**
     * Verificar si un email está registrado
     */
    @GetMapping("/check-email")
    public Map<String, Object> checkEmail(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        boolean isRegistered = authService.isEmailRegistered(email);
        
        response.put("email", email);
        response.put("isRegistered", isRegistered);
        response.put("message", isRegistered ? "Email ya registrado" : "Email disponible");
        
        return response;
    }

    /**
     * Verificar si un username está en uso
     */
    @GetMapping("/check-username")
    public Map<String, Object> checkUsername(@RequestParam String username) {
        Map<String, Object> response = new HashMap<>();
        boolean isTaken = authService.isUsernameTaken(username);
        
        response.put("username", username);
        response.put("isTaken", isTaken);
        response.put("message", isTaken ? "Username no disponible" : "Username disponible");
        
        return response;
    }

    @Autowired
    private tech.nocountry.onboarding.services.EmailVerificationService emailVerificationService;

    @Autowired
    private tech.nocountry.onboarding.services.RefreshTokenService refreshTokenService;

    @Autowired
    private tech.nocountry.onboarding.security.JwtService jwtService;

    @Autowired
    private tech.nocountry.onboarding.services.CaptchaService captchaService;

    /**
     * Verificar email con token
     */
    @PostMapping("/verify-email")
    public Map<String, Object> verifyEmail(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String token = request.get("token");
        
        if (token == null || token.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Token es requerido");
            return response;
        }

        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        boolean verified = emailVerificationService.verifyEmail(token, ipAddress, userAgent);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", verified);
        response.put("message", verified ? "Email verificado exitosamente" : "Token inválido o expirado");
        
        return response;
    }

    /**
     * Reenviar token de verificación de email
     */
    @PostMapping("/resend-verification")
    public Map<String, Object> resendVerification(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String userId = request.get("userId");
        String email = request.get("email");
        
        if ((userId == null || userId.isEmpty()) && (email == null || email.isEmpty())) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "userId o email es requerido");
            return response;
        }

        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        try {
            if (userId != null && !userId.isEmpty()) {
                emailVerificationService.resendVerificationToken(userId, ipAddress, userAgent);
            } else {
                // Buscar userId por email
                java.util.Optional<tech.nocountry.onboarding.entities.User> userOptional = 
                    authService.findUserByEmail(email);
                if (userOptional.isEmpty()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Usuario no encontrado");
                    return response;
                }
                tech.nocountry.onboarding.entities.User user = userOptional.get();
                emailVerificationService.resendVerificationToken(user.getUserId(), ipAddress, userAgent);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Token de verificación enviado. Por favor, revisa tu email.");
            
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al reenviar token: " + e.getMessage());
            return response;
        }
    }

    /**
     * Refrescar JWT token usando refresh token
     */
    @PostMapping("/refresh-token")
    public Map<String, Object> refreshToken(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String refreshToken = request.get("refreshToken");
        
        if (refreshToken == null || refreshToken.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Refresh token es requerido");
            return response;
        }

        try {
            java.util.Optional<tech.nocountry.onboarding.entities.RefreshToken> tokenOptional = 
                refreshTokenService.validateRefreshToken(refreshToken);
            
            if (tokenOptional.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Refresh token inválido o expirado");
                return response;
            }

            tech.nocountry.onboarding.entities.RefreshToken refreshTokenEntity = tokenOptional.get();
            
            // Obtener usuario
            java.util.Optional<tech.nocountry.onboarding.entities.User> userOptional = 
                authService.findUserById(refreshTokenEntity.getUserId());
            
            if (userOptional.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Usuario no encontrado");
                return response;
            }

            tech.nocountry.onboarding.entities.User user = userOptional.get();
            
            // Generar nuevo JWT token (duración corta para sistema bancario)
            String newToken = jwtService.generateToken(user);
            
            // Revocar refresh token usado y generar uno nuevo (opcional)
            // refreshTokenService.revokeRefreshToken(refreshToken);
            // tech.nocountry.onboarding.entities.RefreshToken newRefreshToken = 
            //     refreshTokenService.generateRefreshToken(user.getUserId(), ipAddress, userAgent, refreshTokenEntity.getDeviceId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Token refrescado exitosamente");
            response.put("token", newToken);
            // response.put("refreshToken", newRefreshToken.getToken()); // Si se genera uno nuevo
            
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al refrescar token: " + e.getMessage());
            return response;
        }
    }

    /**
     * Revocar refresh token
     */
    @PostMapping("/revoke-token")
    public Map<String, Object> revokeToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        
        if (refreshToken == null || refreshToken.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Refresh token es requerido");
            return response;
        }

        try {
            refreshTokenService.revokeRefreshToken(refreshToken);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Refresh token revocado exitosamente");
            
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al revocar token: " + e.getMessage());
            return response;
        }
    }

    /**
     * Verificar si se requiere CAPTCHA
     */
    @GetMapping("/captcha-required")
    public Map<String, Object> checkCaptchaRequired(@RequestParam(required = false) String email, 
                                                     HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        boolean required = captchaService.isCaptchaRequired(email, ipAddress);
        
        Map<String, Object> response = new HashMap<>();
        response.put("captchaRequired", required);
        if (required) {
            response.put("message", "Se requiere verificación CAPTCHA debido a múltiples intentos fallidos");
            response.put("remainingAttempts", Math.max(0, captchaService.getRequiredAfterAttempts()));
        }
        
        return response;
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
