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

            // Generate JWT token for registration
            String token = jwtService.generateToken(savedUser);

            return AuthResponse.success(
                "Usuario registrado exitosamente",
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
                return AuthResponse.error("Credenciales inválidas");
            }
            
            System.out.println("DEBUG: Password matches successfully");

            // Actualizar el último login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Generar JWT token
            String token = jwtService.generateToken(user);

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

            // Actualizar el último login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Generar JWT token
            String token = jwtService.generateToken(user);

            // Crear sesión
            sessionService.createSession(user.getUserId(), token, ipAddress, userAgent);

            // Log de seguridad - login exitoso
            securityAuditService.logSecurityEvent(user.getUserId(), "LOGIN_SUCCESS", ipAddress, userAgent, 
                "Login exitoso", "LOW");

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
     * Generar token de recuperación de contraseña
     */
    public String generatePasswordResetToken(String email, String ipAddress, String userAgent) {
        return passwordResetService.generateResetToken(email, ipAddress, userAgent);
    }

    /**
     * Cambiar contraseña usando token de recuperación
     */
    public boolean resetPasswordWithToken(String token, String newPassword, String ipAddress, String userAgent) {
        return passwordResetService.resetPassword(token, passwordEncoder.encode(newPassword), ipAddress, userAgent);
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

        // Cambiar contraseña
        user.setPasswordHash(passwordEncoder.encode(newPass));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Invalidar todas las sesiones del usuario
        sessionService.invalidateAllUserSessions(userId);

        // Log de seguridad
        securityAuditService.logPasswordChange(userId, ipAddress, userAgent);

        return true;
    }

    /**
     * Cerrar sesión (invalidar token)
     */
    public boolean logout(String token) {
        return sessionService.invalidateSession(token);
    }

    /**
     * Obtener sesiones activas de un usuario
     */
    public List<tech.nocountry.onboarding.entities.UserSession> getUserActiveSessions(String userId) {
        return sessionService.getUserActiveSessions(userId);
    }
}
