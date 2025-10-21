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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

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

            // Obtener rol por defecto (APPLICANT)
            Role defaultRole = roleRepository.findByRoleId("ROLE_APPLICANT")
                    .orElseThrow(() -> new RuntimeException("Rol por defecto no encontrado"));

            // Crear el nuevo usuario
            User user = User.builder()
                    .userId(UUID.randomUUID().toString()) // Generar UUID como String
                    .dni(request.getDni())
                    .fullName(request.getFullName())
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
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
     * Autentica un usuario con email y contraseña
     */
    public AuthResponse loginUser(LoginRequest request) {
        try {
            // Buscar el usuario por email
            Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
            
            if (userOptional.isEmpty()) {
                return AuthResponse.error("Credenciales inválidas");
            }

            User user = userOptional.get();

            // Verificar que el usuario esté activo
            if (!user.getIsActive()) {
                return AuthResponse.error("La cuenta está desactivada");
            }

            // Verificar la contraseña
            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                return AuthResponse.error("Credenciales inválidas");
            }

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
        
        AuthResponse response = loginUser(request);
        
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
}
