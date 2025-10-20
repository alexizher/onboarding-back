package tech.nocountry.onboarding.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.repositories.UserRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Registra un nuevo usuario en el sistema
     */
    public Map<String, Object> registerUser(String username, String email, String password, 
                                          String fullName, String phone, String dni) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validar que el username no esté en uso
            if (userRepository.existsByUsername(username)) {
                response.put("success", false);
                response.put("message", "El nombre de usuario ya está en uso");
                return response;
            }

            // Validar que el email no esté en uso
            if (userRepository.existsByEmail(email)) {
                response.put("success", false);
                response.put("message", "El email ya está registrado");
                return response;
            }

            // Crear el nuevo usuario
            User user = User.builder()
                    .username(username)
                    .email(email)
                    .passwordHash(passwordEncoder.encode(password))
                    .fullName(fullName)
                    .phone(phone)
                    .dni(dni)
                    .isActive(true)
                    .consentGdpr(false) // Por defecto false, se puede cambiar después
                    .createdAt(LocalDateTime.now())
                    .build();

            // Guardar el usuario
            User savedUser = userRepository.save(user);

            response.put("success", true);
            response.put("message", "Usuario registrado exitosamente");
            response.put("userId", savedUser.getUserId());
            response.put("email", savedUser.getEmail());
            response.put("username", savedUser.getUsername());

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error en el registro: " + e.getMessage());
        }

        return response;
    }

    /**
     * Autentica un usuario con email y contraseña
     */
    public Map<String, Object> loginUser(String email, String password) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Buscar el usuario por email
            Optional<User> userOptional = userRepository.findByEmail(email);
            
            if (userOptional.isEmpty()) {
                response.put("success", false);
                response.put("message", "Credenciales inválidas");
                return response;
            }

            User user = userOptional.get();

            // Verificar que el usuario esté activo
            if (!user.getIsActive()) {
                response.put("success", false);
                response.put("message", "La cuenta está desactivada");
                return response;
            }

            // Verificar la contraseña
            if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                response.put("success", false);
                response.put("message", "Credenciales inválidas");
                return response;
            }

            // Actualizar el último login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            response.put("success", true);
            response.put("message", "Login exitoso");
            response.put("userId", user.getUserId());
            response.put("email", user.getEmail());
            response.put("username", user.getUsername());
            response.put("fullName", user.getFullName());

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error en el login: " + e.getMessage());
        }

        return response;
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
