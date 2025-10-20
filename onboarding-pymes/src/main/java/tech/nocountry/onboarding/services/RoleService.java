package tech.nocountry.onboarding.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.nocountry.onboarding.entities.Role;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.repositories.RoleRepository;
import tech.nocountry.onboarding.repositories.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Crear un nuevo rol
     */
    public Role createRole(String roleId, String name, String description, String permissionsJson) {
        if (roleRepository.existsByRoleId(roleId)) {
            throw new IllegalArgumentException("El rol con ID " + roleId + " ya existe");
        }

        Role role = Role.builder()
                .roleId(roleId)
                .name(name)
                .description(description)
                .permissions(permissionsJson)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        return roleRepository.save(role);
    }

    /**
     * Crear roles predefinidos para fintech
     */
    public void createDefaultRoles() {
        // Crear rol APPLICANT
        if (!roleRepository.existsByRoleId("ROLE_APPLICANT")) {
            createRole(
                "ROLE_APPLICANT",
                "Solicitante",
                "PYME que solicita crédito",
                "{\"permissions\":[\"CREATE_APPLICATION\",\"UPLOAD_DOCUMENTS\",\"SIGN_DOCUMENTS\",\"VIEW_OWN_APPLICATIONS\"]}"
            );
        }

        // Crear rol ANALYST
        if (!roleRepository.existsByRoleId("ROLE_ANALYST")) {
            createRole(
                "ROLE_ANALYST",
                "Analista",
                "Analista que revisa solicitudes",
                "{\"permissions\":[\"VIEW_ALL_APPLICATIONS\",\"UPDATE_APPLICATION_STATUS\",\"ASSIGN_TASKS\",\"VERIFY_DOCUMENTS\",\"VIEW_REPORTS\"]}"
            );
        }

        // Crear rol MANAGER
        if (!roleRepository.existsByRoleId("ROLE_MANAGER")) {
            createRole(
                "ROLE_MANAGER",
                "Gerente",
                "Gerente que aprueba solicitudes",
                "{\"permissions\":[\"APPROVE_APPLICATIONS\",\"REJECT_APPLICATIONS\",\"VIEW_ALL_APPLICATIONS\",\"VIEW_REPORTS\",\"MANAGE_ANALYSTS\",\"CONFIGURE_PARAMETERS\"]}"
            );
        }

        // Crear rol ADMIN
        if (!roleRepository.existsByRoleId("ROLE_ADMIN")) {
            createRole(
                "ROLE_ADMIN",
                "Administrador",
                "Administrador del sistema",
                "{\"permissions\":[\"FULL_ACCESS\",\"MANAGE_USERS\",\"MANAGE_ROLES\",\"VIEW_ALL_DATA\",\"SYSTEM_CONFIGURATION\"]}"
            );
        }
    }

    /**
     * Obtener todos los roles activos
     */
    public List<Role> getAllActiveRoles() {
        return roleRepository.findByIsActiveTrue();
    }

    /**
     * Obtener rol por ID
     */
    public Optional<Role> getRoleById(String roleId) {
        return roleRepository.findByRoleId(roleId);
    }

    /**
     * Actualizar rol
     */
    public Role updateRole(String roleId, String name, String description, String permissionsJson) {
        Role role = roleRepository.findByRoleId(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + roleId));

        role.setName(name);
        role.setDescription(description);
        role.setPermissions(permissionsJson);
        role.setUpdatedAt(LocalDateTime.now());

        return roleRepository.save(role);
    }

    /**
     * Desactivar rol
     */
    public boolean deactivateRole(String roleId) {
        Role role = roleRepository.findByRoleId(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + roleId));

        role.setIsActive(false);
        role.setUpdatedAt(LocalDateTime.now());
        roleRepository.save(role);

        return true;
    }

    /**
     * Asignar rol a usuario
     */
    public User assignRoleToUser(String userId, String roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));

        Role role = roleRepository.findByRoleId(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + roleId));

        user.setRole(role);
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    /**
     * Obtener rol de un usuario
     */
    public Optional<Role> getUserRole(String userId) {
        return roleRepository.findRoleByUserId(userId);
    }

    /**
     * Verificar si usuario tiene un rol específico
     */
    public boolean userHasRole(String userId, String roleId) {
        return roleRepository.userHasRole(userId, roleId);
    }

    /**
     * Verificar si usuario tiene un permiso específico
     */
    public boolean userHasPermission(String userId, String permission) {
        Optional<Role> userRole = getUserRole(userId);
        if (userRole.isEmpty()) {
            return false;
        }

        String permissionsJson = userRole.get().getPermissions();
        if (permissionsJson == null || permissionsJson.isEmpty()) {
            return false;
        }

        // Verificar si el permiso está en el JSON
        return permissionsJson.contains("\"" + permission + "\"");
    }

    /**
     * Obtener información completa de rol de un usuario
     */
    public UserRoleInfo getUserRoleInfo(String userId) {
        Optional<Role> role = getUserRole(userId);
        
        return UserRoleInfo.builder()
                .userId(userId)
                .role(role.orElse(null))
                .build();
    }

    // Clase interna para información de rol del usuario
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserRoleInfo {
        private String userId;
        private Role role;
    }
}