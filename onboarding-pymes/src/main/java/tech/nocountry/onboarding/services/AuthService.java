package tech.nocountry.onboarding.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tech.nocountry.onboarding.dto.AuthResponse;
import tech.nocountry.onboarding.dto.LoginRequest;
import tech.nocountry.onboarding.dto.RegisterRequest;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.entities.Role;
import tech.nocountry.onboarding.repositories.UserRepository;
import tech.nocountry.onboarding.repositories.RoleRepository;
import tech.nocountry.onboarding.security.JwtService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    public PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private RateLimitingService rateLimitingService;

    @Autowired
    private SecurityAuditService securityAuditService;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private PasswordValidationService passwordValidationService;

    @Autowired
    private tech.nocountry.onboarding.repositories.ClientBlacklistRepository clientBlacklistRepository;

    @Autowired
    private AccountLockoutService accountLockoutService;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private PasswordHistoryService passwordHistoryService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private tech.nocountry.onboarding.repositories.PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private NotificationService notificationService;

    // Constante para mostrar mensajes de intentos restantes
    private static final int MAX_ATTEMPTS_BEFORE_LOCKOUT = 3;

    /**
     * Registra un nuevo usuario en el sistema
     */
    public AuthResponse registerUser(RegisterRequest request) {
        try {
            // Validar que el username no esté en uso
            if (userRepository.existsByUsername(request.getUsername())) {
                return AuthResponse.error("El nombre de usuario ya está en uso");
            }

            // Validar que el email no esté en uso
            if (userRepository.existsByEmail(request.getEmail())) {
                return AuthResponse.error("El email ya está registrado");
            }

            // Validar fortaleza de la contraseña
            PasswordValidationService.PasswordValidationResult passwordValidation = 
                passwordValidationService.validatePassword(request.getPassword());
            
            if (!passwordValidation.isValid()) {
                return AuthResponse.error("Contraseña no válida: " + 
                    String.join(", ", passwordValidation.getErrors()));
            }

            // Obtener rol por defecto (APPLICANT)
            Role defaultRole = roleRepository.findByRoleId("ROLE_APPLICANT")
                    .orElseThrow(() -> new RuntimeException("Rol por defecto no encontrado"));

            // Crear el nuevo usuario
            // Truncar contraseña a 72 bytes para compatibilidad con BCrypt
            String password = request.getPassword();
            if (password.getBytes().length > 72) {
                password = new String(password.getBytes(), 0, 72);
            }
            
            User user = User.builder()
                    .userId(UUID.randomUUID().toString()) // Generar UUID como String
                    .dni(request.getDni())
                    .fullName(request.getFullName())
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .passwordHash(passwordEncoder.encode(password))
                    .phone(request.getPhone())
                    .role(defaultRole) // Asignar rol por defecto
                    .isActive(true)
                    .consentGdpr(false) // Por defecto false, se puede cambiar después
                    .createdAt(LocalDateTime.now())
                    .build();

            // Guardar el usuario
            User savedUser = userRepository.save(user);

            // Guardar contraseña en historial
            passwordHistoryService.addPasswordToHistory(savedUser.getUserId(), savedUser.getPasswordHash());

            // Generar token de verificación de email
            try {
                tech.nocountry.onboarding.entities.EmailVerificationToken verificationToken = 
                    emailVerificationService.generateVerificationToken(
                        savedUser.getUserId(), 
                        savedUser.getEmail(), 
                        null, 
                        null
                    );
                // Enviar notificación de verificación de email
                if (verificationToken != null) {
                    notificationService.notifyEmailVerification(savedUser.getUserId(), verificationToken.getToken());
                    notificationService.notifyRegistration(savedUser.getUserId());
                }
            } catch (Exception e) {
                // No fallar el registro si hay error al generar token de verificación
                System.out.println("Error al generar token de verificación: " + e.getMessage());
            }

            // Generate JWT token for registration
            String token = jwtService.generateToken(savedUser);

            return AuthResponse.success(
                "Usuario registrado exitosamente. Por favor, verifica tu email.",
                savedUser.getUserId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                token
            );

        } catch (Exception e) {
            return AuthResponse.error("Error en el registro: " + e.getMessage());
        }
    }

    /**
     * Método de compatibilidad para el controlador actual
     */
    public Map<String, Object> registerUser(String username, String email, String password, String fullName, String phone, String dni) {
        RegisterRequest request = RegisterRequest.builder()
                .dni(dni)
                .fullName(fullName)
                .username(username)
                .email(email)
                .password(password)
                .phone(phone)
                .build();
        
        AuthResponse response = registerUser(request);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", response.isSuccess());
        result.put("message", response.getMessage());
        if (response.getUserId() != null) {
            result.put("userId", response.getUserId());
            result.put("email", response.getEmail());
            result.put("username", response.getUsername());
            result.put("token", response.getToken());
        }
        
        return result;
    }

    /**
     * Autentica un usuario con email y contraseña (versión simple sin seguridad avanzada)
     */
    public AuthResponse loginUserSimple(LoginRequest request) {
        System.out.println("DEBUG: Starting simple login process for email: " + request.getEmail());
        try {
            // Buscar el usuario por email
            System.out.println("DEBUG: Searching for user with email: " + request.getEmail());
            Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
            System.out.println("DEBUG: User found: " + userOptional.isPresent());
            
            if (userOptional.isEmpty()) {
                System.out.println("DEBUG: User not found");
                return AuthResponse.error("Credenciales inválidas");
            }

            User user = userOptional.get();

            // Verificar que el usuario esté activo
            if (!user.getIsActive()) {
                return AuthResponse.error("La cuenta está desactivada");
            }

            // Verificar que el usuario no esté en blacklist
            if (clientBlacklistRepository.isUserBlacklisted(user.getUserId())) {
                System.out.println("DEBUG: User is blacklisted");
                return AuthResponse.error("Usuario bloqueado: no puede acceder al sistema");
            }

            // Verificar bloqueo progresivo de cuenta (versión simple - sin IP/UA)
            if (accountLockoutService.isAccountLocked(user)) {
                // Si intentan login durante bloqueo, incrementar nivel
                accountLockoutService.recordFailedAttempt(user, null, null);
                String lockoutMessage = accountLockoutService.getLockoutMessage(user);
                return AuthResponse.error(lockoutMessage != null ? lockoutMessage : 
                    "Cuenta bloqueada temporalmente por seguridad. Intenta más tarde.");
            }

            // Verificar la contraseña
            System.out.println("DEBUG: About to verify password");
            // Truncar contraseña a 72 bytes para compatibilidad con BCrypt
            String password = request.getPassword();
            if (password.getBytes().length > 72) {
                password = new String(password.getBytes(), 0, 72);
            }
            boolean passwordMatch = passwordEncoder.matches(password, user.getPasswordHash());
            System.out.println("DEBUG: Password match result: " + passwordMatch);
            
            if (!passwordMatch) {
                System.out.println("DEBUG: Password does not match");
                // Registrar intento fallido en versión simple también
                accountLockoutService.recordFailedAttempt(user, null, null);
                
                // Verificar si se bloqueó después de este intento
                if (accountLockoutService.isAccountLocked(user)) {
                    String lockoutMessage = accountLockoutService.getLockoutMessage(user);
                    return AuthResponse.error(lockoutMessage != null ? lockoutMessage : 
                        "Cuenta bloqueada por múltiples intentos fallidos. Intenta más tarde.");
                }
                
                return AuthResponse.error("Credenciales inválidas");
            }
            
            System.out.println("DEBUG: Password matches successfully");
            
            // Resetear bloqueo progresivo al tener login exitoso (versión simple)
            accountLockoutService.resetLockout(user);

            // Actualizar el último login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Generar JWT token
            String token = jwtService.generateToken(user);

            // Crear sesión (obtener IP y UserAgent del contexto si están disponibles)
            // Nota: En loginUserSimple no tenemos HttpServletRequest, así que usamos valores por defecto
            try {
                String ipAddress = "127.0.0.1"; // IP por defecto
                String userAgent = "API-Test"; // User Agent por defecto
                sessionService.createSession(user.getUserId(), token, ipAddress, userAgent);
            } catch (Exception e) {
                System.out.println("DEBUG: Error al crear sesión (no crítico): " + e.getMessage());
                // No fallar el login si hay error al crear sesión
            }

            return AuthResponse.success(
                "Login exitoso",
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                token
            );

        } catch (Exception e) {
            System.out.println("DEBUG: Exception in simple login: " + e.getMessage());
            return AuthResponse.error("Error en el login: " + e.getMessage());
        }
    }

    /**
     * Autentica un usuario con email y contraseña (versión completa con seguridad)
     */
    public AuthResponse loginUser(LoginRequest request, String ipAddress, String userAgent) {
        System.out.println("DEBUG: Starting login process for email: " + request.getEmail());
        try {
            // Verificar rate limiting
            if (!rateLimitingService.canAttemptLogin(ipAddress, request.getEmail())) {
                System.out.println("DEBUG: Rate limiting blocked");
                return AuthResponse.error("Demasiados intentos de login. Intenta más tarde.");
            }

            // Buscar el usuario por email
            System.out.println("DEBUG: Searching for user with email: " + request.getEmail());
            Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
            System.out.println("DEBUG: User found: " + userOptional.isPresent());
            
            if (userOptional.isEmpty()) {
                System.out.println("DEBUG: User not found");
                rateLimitingService.recordLoginAttempt(ipAddress, request.getEmail(), false, 
                    "Usuario no encontrado", userAgent);
                return AuthResponse.error("Credenciales inválidas");
            }

            User user = userOptional.get();

            // Verificar que el usuario esté activo
            if (!user.getIsActive()) {
                rateLimitingService.recordLoginAttempt(ipAddress, request.getEmail(), false, 
                    "Cuenta desactivada", userAgent);
                return AuthResponse.error("La cuenta está desactivada");
            }

            // Verificar que el usuario no esté en blacklist
            if (clientBlacklistRepository.isUserBlacklisted(user.getUserId())) {
                System.out.println("DEBUG: User is blacklisted");
                rateLimitingService.recordLoginAttempt(ipAddress, request.getEmail(), false, 
                    "Usuario bloqueado", userAgent);
                securityAuditService.logSecurityEvent(user.getUserId(), "LOGIN_BLOCKED", ipAddress, userAgent, 
                    "Intento de login de usuario bloqueado", "HIGH");
                return AuthResponse.error("Usuario bloqueado: no puede acceder al sistema");
            }

            // Verificar la contraseña
            System.out.println("DEBUG: About to verify password");
            System.out.println("DEBUG: Password length: " + request.getPassword().length());
            System.out.println("DEBUG: Password bytes: " + request.getPassword().getBytes().length);
            System.out.println("DEBUG: Password: " + request.getPassword());
            System.out.println("DEBUG: Hash length: " + user.getPasswordHash().length());
            System.out.println("DEBUG: Hash: " + user.getPasswordHash());
            
            try {
                // Truncar contraseña a 72 bytes para compatibilidad con BCrypt
                String password = request.getPassword();
                if (password.getBytes().length > 72) {
                    password = new String(password.getBytes(), 0, 72);
                }
                boolean passwordMatch = passwordEncoder.matches(password, user.getPasswordHash());
                System.out.println("DEBUG: Password match result: " + passwordMatch);
                
                if (!passwordMatch) {
                    System.out.println("DEBUG: Password does not match");
                    rateLimitingService.recordLoginAttempt(ipAddress, request.getEmail(), false, 
                        "Contraseña incorrecta", userAgent);
                    
                    // Registrar intento fallido y aplicar bloqueo progresivo si es necesario
                    accountLockoutService.recordFailedAttempt(user, ipAddress, userAgent);
                    
                    // Verificar si se bloqueó después de este intento
                    if (accountLockoutService.isAccountLocked(user)) {
                        String lockoutMessage = accountLockoutService.getLockoutMessage(user);
                        
                        // Notificar bloqueo de cuenta
                        try {
                            notificationService.notifyAccountLocked(user.getUserId(), 
                                "Múltiples intentos fallidos de login");
                        } catch (Exception e) {
                            // No fallar si hay error al enviar notificación
                            System.out.println("Error al enviar notificación de bloqueo: " + e.getMessage());
                        }
                        
                        return AuthResponse.error(lockoutMessage != null ? lockoutMessage : 
                            "Cuenta bloqueada por múltiples intentos fallidos. Intenta más tarde.");
                    }
                    
                    // Informar cuántos intentos restantes
                    int remainingAttempts = MAX_ATTEMPTS_BEFORE_LOCKOUT - 
                        (user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0);
                    if (remainingAttempts > 0 && remainingAttempts <= MAX_ATTEMPTS_BEFORE_LOCKOUT) {
                        return AuthResponse.error(String.format(
                            "Credenciales inválidas. Te quedan %d intento(s) antes del bloqueo.", remainingAttempts));
                    }
                    
                    return AuthResponse.error("Credenciales inválidas");
                }
                
                System.out.println("DEBUG: Password matches successfully");
            } catch (Exception e) {
                System.out.println("DEBUG: Exception in password verification: " + e.getMessage());
                System.out.println("DEBUG: Exception type: " + e.getClass().getName());
                rateLimitingService.recordLoginAttempt(ipAddress, request.getEmail(), false, 
                    "Error en verificación de contraseña: " + e.getMessage(), userAgent);
                return AuthResponse.error("Error en el login: " + e.getMessage());
            }

            // Login exitoso
            rateLimitingService.recordLoginAttempt(ipAddress, request.getEmail(), true, null, userAgent);

            // Resetear bloqueo progresivo al tener login exitoso
            accountLockoutService.resetLockout(user);

            // Actualizar el último login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Generar JWT token (duración corta para sistema bancario - 30 min)
            String token = jwtService.generateToken(user);

            // Generar refresh token (duración corta para sistema bancario - 30 min)
            String deviceId = request.getDeviceId() != null ? request.getDeviceId() : "unknown";
            try {
                refreshTokenService.generateRefreshToken(user.getUserId(), ipAddress, userAgent, deviceId);
            } catch (Exception e) {
                // No fallar el login si hay error al generar refresh token
                System.out.println("Error al generar refresh token: " + e.getMessage());
            }

            // Crear sesión
            sessionService.createSession(user.getUserId(), token, ipAddress, userAgent);

            // Log de seguridad - login exitoso
            securityAuditService.logSecurityEvent(user.getUserId(), "LOGIN_SUCCESS", ipAddress, userAgent, 
                "Login exitoso", "LOW");

            // Notificar nuevo login (opcional - puede verificarse si es desde dispositivo nuevo)
            try {
                notificationService.notifyNewLogin(user.getUserId(), ipAddress, userAgent);
            } catch (Exception e) {
                // No fallar el login si hay error al enviar notificación
                System.out.println("Error al enviar notificación de login: " + e.getMessage());
            }

            return AuthResponse.success(
                "Login exitoso",
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                token
            );

        } catch (Exception e) {
            rateLimitingService.recordLoginAttempt(ipAddress, request.getEmail(), false, 
                "Error del sistema: " + e.getMessage(), userAgent);
            return AuthResponse.error("Error en el login: " + e.getMessage());
        }
    }

    /**
     * Método de compatibilidad para el controlador actual
     */
    public Map<String, Object> loginUser(String email, String password) {
        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();
        
        // Usar IP y User Agent por defecto para compatibilidad
        AuthResponse response = loginUser(request, "127.0.0.1", "Compatibility-Method");
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", response.isSuccess());
        result.put("message", response.getMessage());
        if (response.getUserId() != null) {
            result.put("userId", response.getUserId());
            result.put("email", response.getEmail());
            result.put("username", response.getUsername());
            result.put("fullName", response.getFullName());
            result.put("token", response.getToken());
        }
        
        return result;
    }

    /**
     * Verifica si un email ya está registrado
     */
    public boolean isEmailRegistered(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Verifica si un username ya está en uso
     */
    public boolean isUsernameTaken(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Buscar usuario por email
     */
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Buscar usuario por ID
     */
    public Optional<User> findUserById(String userId) {
        return userRepository.findById(userId);
    }

    /**
     * Generar token de recuperación de contraseña
     */
    public String generatePasswordResetToken(String email, String ipAddress, String userAgent) {
        String token = passwordResetService.generateResetToken(email, ipAddress, userAgent);
        
        // Notificar por email si se generó el token exitosamente
        if (token != null) {
            try {
                java.util.Optional<User> userOptional = userRepository.findByEmail(email);
                if (userOptional.isPresent()) {
                    notificationService.notifyPasswordReset(userOptional.get().getUserId(), token);
                }
            } catch (Exception e) {
                // No fallar si hay error al enviar notificación
                System.out.println("Error al enviar notificación de password reset: " + e.getMessage());
            }
        }
        
        return token;
    }

    /**
     * Cambiar contraseña usando token de recuperación
     */
    public boolean resetPasswordWithToken(String token, String newPassword, String ipAddress, String userAgent) {
        // Validar que la nueva contraseña no esté en el historial
        String newPasswordHash = passwordEncoder.encode(newPassword);
        
        // Obtener userId del token antes de resetear para validar historial
        java.util.Optional<tech.nocountry.onboarding.entities.PasswordResetToken> tokenOptional = 
            passwordResetTokenRepository.findByToken(token);
        
        if (tokenOptional.isEmpty()) {
            return false;
        }
        
        String userId = tokenOptional.get().getUserId();
        
        // Verificar historial antes de resetear
        if (passwordHistoryService.isPasswordInHistory(userId, newPasswordHash)) {
            securityAuditService.logSecurityEvent(userId, "PASSWORD_RESET_FAILED", ipAddress, userAgent, 
                "Intento de reset con contraseña reutilizada del historial", "MEDIUM");
            return false;
        }
        
        // Resetear contraseña
        boolean success = passwordResetService.resetPassword(token, newPasswordHash, ipAddress, userAgent);
        
        if (success) {
            // Agregar nueva contraseña al historial
            passwordHistoryService.addPasswordToHistory(userId, newPasswordHash);
        }
        
        return success;
    }

    /**
     * Cambiar contraseña de usuario autenticado
     */
    public boolean changePassword(String userId, String currentPassword, String newPassword, String ipAddress, String userAgent) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();
        
        // Verificar contraseña actual
        // Truncar contraseña a 72 bytes para compatibilidad con BCrypt
        String currentPass = currentPassword;
        if (currentPass.getBytes().length > 72) {
            currentPass = new String(currentPass.getBytes(), 0, 72);
        }
        if (!passwordEncoder.matches(currentPass, user.getPasswordHash())) {
            securityAuditService.logSecurityEvent(userId, "PASSWORD_CHANGE_FAILED", ipAddress, userAgent, 
                "Intento de cambio de contraseña con contraseña actual incorrecta", "MEDIUM");
            return false;
        }

        // Validar nueva contraseña
        PasswordValidationService.PasswordValidationResult passwordValidation = 
            passwordValidationService.validatePassword(newPassword);
        
        if (!passwordValidation.isValid()) {
            securityAuditService.logSecurityEvent(userId, "PASSWORD_CHANGE_FAILED", ipAddress, userAgent, 
                "Intento de cambio de contraseña con contraseña débil: " + 
                String.join(", ", passwordValidation.getErrors()), "MEDIUM");
            return false;
        }

        // Verificar que la nueva contraseña no sea igual a la actual
        // Truncar nueva contraseña a 72 bytes para compatibilidad con BCrypt
        String newPass = newPassword;
        if (newPass.getBytes().length > 72) {
            newPass = new String(newPass.getBytes(), 0, 72);
        }
        if (passwordEncoder.matches(newPass, user.getPasswordHash())) {
            securityAuditService.logSecurityEvent(userId, "PASSWORD_CHANGE_FAILED", ipAddress, userAgent, 
                "Intento de cambio de contraseña con la misma contraseña actual", "LOW");
            return false;
        }

        // Verificar que la nueva contraseña no esté en el historial
        String newPasswordHash = passwordEncoder.encode(newPass);
        if (passwordHistoryService.isPasswordInHistory(userId, newPasswordHash)) {
            securityAuditService.logSecurityEvent(userId, "PASSWORD_CHANGE_FAILED", ipAddress, userAgent, 
                "Intento de cambio de contraseña con contraseña reutilizada del historial", "MEDIUM");
            return false;
        }

        // Guardar contraseña actual en historial antes de cambiarla
        passwordHistoryService.addPasswordToHistory(userId, user.getPasswordHash());

        // Cambiar contraseña
        user.setPasswordHash(newPasswordHash);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        // Invalidar todas las sesiones del usuario
        sessionService.invalidateAllUserSessions(userId);

        // Notificar cambio de contraseña
        try {
            notificationService.notifyPasswordChange(userId, ipAddress);
        } catch (Exception e) {
            // No fallar el cambio de contraseña si hay error al enviar notificación
            System.out.println("Error al enviar notificación de cambio de contraseña: " + e.getMessage());
        }

        // Nota: Los tokens JWT existentes no pueden ser revocados directamente
        // porque no los almacenamos en sesiones. Sin embargo, al invalidar las sesiones,
        // los tokens asociados ya no funcionarán. Si necesitas revocar tokens inmediatamente,
        // deberías implementar un sistema para trackear todos los tokens activos del usuario.
        // Por ahora, la invalidación de sesiones es suficiente ya que los tokens están
        // asociados a las sesiones.

        // Log de seguridad
        securityAuditService.logPasswordChange(userId, ipAddress, userAgent);

        return true;
    }

    /**
     * Cerrar sesión (invalidar token y agregar a blacklist)
     */
    public boolean logout(String token) {
        try {
            // Invalidar sesión
            boolean sessionInvalidated = sessionService.invalidateSession(token);
            
            // Agregar token a blacklist para revocarlo
            if (sessionInvalidated) {
                String userId = jwtService.extractUserId(token);
                if (userId != null) {
                    jwtService.blacklistToken(token, userId, "LOGOUT");
                    
                    // Log de seguridad
                    securityAuditService.logSecurityEvent(
                        userId,
                        "LOGOUT",
                        null,
                        null,
                        "Usuario cerró sesión",
                        "LOW"
                    );
                }
            }
            
            return sessionInvalidated;
        } catch (Exception e) {
            System.err.println("Error en logout: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtener sesiones activas de un usuario
     */
    public List<tech.nocountry.onboarding.entities.UserSession> getUserActiveSessions(String userId) {
        return sessionService.getUserActiveSessions(userId);
    }

    /**
     * Invalidar una sesión específica
     */
    public boolean invalidateSession(String userId, String sessionId) {
        return sessionService.invalidateSessionById(sessionId, userId);
    }

    /**
     * Cerrar todas las demás sesiones excepto la actual
     */
    public int closeOtherSessions(String userId, String currentToken) {
        String currentTokenHash = sessionService.hashToken(currentToken);
        return sessionService.closeOtherSessions(userId, currentTokenHash);
    }
}
