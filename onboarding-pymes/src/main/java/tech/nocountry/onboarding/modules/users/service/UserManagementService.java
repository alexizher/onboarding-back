package tech.nocountry.onboarding.modules.users.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.nocountry.onboarding.entities.Role;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.modules.users.dto.*;
import tech.nocountry.onboarding.repositories.RoleRepository;
import tech.nocountry.onboarding.repositories.UserRepository;
import tech.nocountry.onboarding.services.PasswordValidationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidationService passwordValidationService;

    /*
     * Obtener todos los usuarios
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /*
     * Obtener usuarios activos
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getActiveUsers() {
        return userRepository.findAll().stream()
                .filter(User::getIsActive)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /*
     * Obtener usuario por ID
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return mapToResponse(user);
    }

    /*
     * Crear nuevo usuario
     */
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        log.info("Creating user: {}", request.getUsername());

        // Validar username único
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("El nombre de usuario ya está en uso");
        }

        // Validar email único
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }

        // Validar contraseña
        PasswordValidationService.PasswordValidationResult passwordValidation =
                passwordValidationService.validatePassword(request.getPassword());
        if (!passwordValidation.isValid()) {
            throw new RuntimeException("Contraseña no válida: " +
                    String.join(", ", passwordValidation.getErrors()));
        }

        // Validar rol
        Role role = roleRepository.findByRoleId(request.getRoleId())
                .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + request.getRoleId()));

        if (!role.getIsActive()) {
            throw new RuntimeException("El rol no está activo");
        }

        // Crear usuario
        String passwordHash = passwordEncoder.encode(request.getPassword());
        User user = User.builder()
                .userId(UUID.randomUUID().toString())
                .fullName(request.getFullName())
                .dni(request.getDni())
                .phone(request.getPhone())
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordHash)
                .isActive(true)
                .consentGdpr(request.getConsentGdpr() != null ? request.getConsentGdpr() : false)
                .role(role)
                .createdAt(LocalDateTime.now())
                .build();

        User saved = userRepository.save(user);
        log.info("User created: {}", saved.getUserId());
        return mapToResponse(saved);
    }

    /*
     * Actualizar usuario
     */
    @Transactional
    public UserResponse updateUser(String userId, UserUpdateRequest request, String currentUserId, boolean isAdmin) {
        log.info("Updating user: {} by: {}", userId, currentUserId);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Validar que el usuario solo puede actualizar su propia info (excepto admin)
        if (!isAdmin && !userId.equals(currentUserId)) {
            throw new RuntimeException("No tienes permiso para actualizar este usuario");
        }

        // Actualizar campos
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getDni() != null) {
            user.setDni(request.getDni());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            // Validar email único si cambió
            if (!user.getEmail().equals(request.getEmail()) && 
                userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("El email ya está en uso");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getIsActive() != null && isAdmin) {
            // Solo admin puede cambiar isActive
            user.setIsActive(request.getIsActive());
        }
        if (request.getConsentGdpr() != null) {
            user.setConsentGdpr(request.getConsentGdpr());
        }
        if (request.getRoleId() != null && isAdmin) {
            // Solo admin puede cambiar rol
            Role role = roleRepository.findByRoleId(request.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + request.getRoleId()));
            if (!role.getIsActive()) {
                throw new RuntimeException("El rol no está activo");
            }
            user.setRole(role);
        }

        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        log.info("User updated: {}", saved.getUserId());
        return mapToResponse(saved);
    }

    /*
     * Cambiar contraseña del usuario
     */
    @Transactional
    public void changePassword(String userId, UserPasswordChangeRequest request) {
        log.info("Changing password for user: {}", userId);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Validar contraseña actual
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("La contraseña actual es incorrecta");
        }

        // Validar nueva contraseña
        PasswordValidationService.PasswordValidationResult passwordValidation =
                passwordValidationService.validatePassword(request.getNewPassword());
        if (!passwordValidation.isValid()) {
            throw new RuntimeException("Contraseña no válida: " +
                    String.join(", ", passwordValidation.getErrors()));
        }

        // Actualizar contraseña
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Password changed for user: {}", userId);
    }

    /*
     * Activar usuario
     */
    @Transactional
    public UserResponse activateUser(String userId) {
        log.info("Activating user: {}", userId);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setIsActive(true);
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        log.info("User activated: {}", saved.getUserId());
        return mapToResponse(saved);
    }

    /*
     * Desactivar usuario
     */
    @Transactional
    public UserResponse deactivateUser(String userId) {
        log.info("Deactivating user: {}", userId);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setIsActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        log.info("User deactivated: {}", saved.getUserId());
        return mapToResponse(saved);
    }

    /*
     * Asignar rol a usuario (solo admin)
     */
    @Transactional
    public UserResponse assignRole(String userId, String roleId) {
        log.info("Assigning role {} to user: {}", roleId, userId);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Role role = roleRepository.findByRoleId(roleId)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + roleId));

        if (!role.getIsActive()) {
            throw new RuntimeException("El rol no está activo");
        }

        user.setRole(role);
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        log.info("Role assigned to user: {}", saved.getUserId());
        return mapToResponse(saved);
    }

    /*
     * Mapping Methods
     */
    private UserResponse mapToResponse(User user) {
        String roleId = null;
        String roleName = null;
        try {
            if (user.getRole() != null) {
                roleId = user.getRole().getRoleId();
                roleName = user.getRole().getName();
            }
        } catch (Exception e) {
            log.warn("Error accessing role for user {}: {}", user.getUserId(), e.getMessage());
        }

        return UserResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .dni(user.getDni())
                .phone(user.getPhone())
                .username(user.getUsername())
                .email(user.getEmail())
                .isActive(user.getIsActive())
                .consentGdpr(user.getConsentGdpr())
                .roleId(roleId)
                .roleName(roleName)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }
}

