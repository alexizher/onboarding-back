package tech.nocountry.onboarding.modules.users.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tech.nocountry.onboarding.dto.ApiResponse;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.modules.users.dto.*;
import tech.nocountry.onboarding.modules.users.service.UserManagementService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UserManagementController {

    private final UserManagementService userManagementService;

    /*
     * Obtener todos los usuarios (solo admin/manager/analyst)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        try {
            List<UserResponse> users = userManagementService.getAllUsers();
            return ResponseEntity.ok(
                ApiResponse.<List<UserResponse>>builder()
                    .success(true)
                    .message("Usuarios obtenidos exitosamente")
                    .data(users)
                    .build()
            );
        } catch (Exception e) {
            log.error("Error getting users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<List<UserResponse>>builder()
                    .success(false)
                    .message("Error al obtener usuarios: " + e.getMessage())
                    .build());
        }
    }

    /*
     * Obtener usuarios activos
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getActiveUsers() {
        try {
            List<UserResponse> users = userManagementService.getActiveUsers();
            return ResponseEntity.ok(
                ApiResponse.<List<UserResponse>>builder()
                    .success(true)
                    .message("Usuarios activos obtenidos exitosamente")
                    .data(users)
                    .build()
            );
        } catch (Exception e) {
            log.error("Error getting active users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<List<UserResponse>>builder()
                    .success(false)
                    .message("Error al obtener usuarios activos: " + e.getMessage())
                    .build());
        }
    }

    /*
     * Obtener usuario por ID
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable String userId,
            Authentication authentication) {
        try {
            String currentUserId = getCurrentUserId();
            boolean isAdmin = isAdmin(authentication);

            // Validar que el usuario solo puede ver su propia info (excepto admin/manager/analyst)
            if (!isAdmin && !isManager(authentication) && !isAnalyst(authentication) && !userId.equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.<UserResponse>builder()
                        .success(false)
                        .message("No tienes permiso para ver este usuario")
                        .build());
            }

            UserResponse user = userManagementService.getUserById(userId);
            return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                    .success(true)
                    .message("Usuario obtenido exitosamente")
                    .data(user)
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<UserResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error getting user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<UserResponse>builder()
                    .success(false)
                    .message("Error al obtener usuario: " + e.getMessage())
                    .build());
        }
    }

    /*
     * Crear nuevo usuario (solo admin)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserCreateRequest request) {
        try {
            UserResponse user = userManagementService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<UserResponse>builder()
                    .success(true)
                    .message("Usuario creado exitosamente")
                    .data(user)
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<UserResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error creating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<UserResponse>builder()
                    .success(false)
                    .message("Error al crear usuario: " + e.getMessage())
                    .build());
        }
    }

    /*
     * Actualizar usuario
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UserUpdateRequest request,
            Authentication authentication) {
        try {
            String currentUserId = getCurrentUserId();
            boolean isAdmin = isAdmin(authentication);

            UserResponse user = userManagementService.updateUser(userId, request, currentUserId, isAdmin);
            return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                    .success(true)
                    .message("Usuario actualizado exitosamente")
                    .data(user)
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<UserResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error updating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<UserResponse>builder()
                    .success(false)
                    .message("Error al actualizar usuario: " + e.getMessage())
                    .build());
        }
    }

    /*
     * Cambiar contraseña del usuario
     */
    @PostMapping("/{userId}/change-password")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable String userId,
            @Valid @RequestBody UserPasswordChangeRequest request,
            Authentication authentication) {
        try {
            String currentUserId = getCurrentUserId();
            boolean isAdmin = isAdmin(authentication);

            // Validar que el usuario solo puede cambiar su propia contraseña (excepto admin)
            if (!isAdmin && !userId.equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.<Void>builder()
                        .success(false)
                        .message("No tienes permiso para cambiar la contraseña de este usuario")
                        .build());
            }

            userManagementService.changePassword(userId, request);
            return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                    .success(true)
                    .message("Contraseña actualizada exitosamente")
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error changing password", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Error al cambiar contraseña: " + e.getMessage())
                    .build());
        }
    }

    /*
     * Activar usuario (solo admin)
     */
    @PostMapping("/{userId}/activate")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(@PathVariable String userId) {
        try {
            UserResponse user = userManagementService.activateUser(userId);
            return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                    .success(true)
                    .message("Usuario activado exitosamente")
                    .data(user)
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<UserResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error activating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<UserResponse>builder()
                    .success(false)
                    .message("Error al activar usuario: " + e.getMessage())
                    .build());
        }
    }

    /*
     * Desactivar usuario (solo admin)
     */
    @PostMapping("/{userId}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(@PathVariable String userId) {
        try {
            UserResponse user = userManagementService.deactivateUser(userId);
            return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                    .success(true)
                    .message("Usuario desactivado exitosamente")
                    .data(user)
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<UserResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error deactivating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<UserResponse>builder()
                    .success(false)
                    .message("Error al desactivar usuario: " + e.getMessage())
                    .build());
        }
    }

    /*
     * Asignar rol a usuario (solo admin)
     */
    @PostMapping("/{userId}/assign-role")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> assignRole(
            @PathVariable String userId,
            @RequestBody Map<String, String> request) {
        try {
            String roleId = request.get("roleId");
            if (roleId == null || roleId.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<UserResponse>builder()
                        .success(false)
                        .message("El roleId es obligatorio")
                        .build());
            }

            UserResponse user = userManagementService.assignRole(userId, roleId);
            return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                    .success(true)
                    .message("Rol asignado exitosamente")
                    .data(user)
                    .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<UserResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error assigning role", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<UserResponse>builder()
                    .success(false)
                    .message("Error al asignar rol: " + e.getMessage())
                    .build());
        }
    }

    /*
     * Helper methods
     */
    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean isManager(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
    }

    private boolean isAnalyst(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ANALYST"));
    }

    /*
     * Helper method to get current user ID
     */
    private String getCurrentUserId() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            
            if (principal instanceof User) {
                return ((User) principal).getUserId();
            }
            
            // Si no es User, intentar obtener desde el nombre de usuario
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            log.warn("Principal is not User instance, using username: {}", username);
            return username;
            
        } catch (Exception e) {
            log.error("Error getting current user ID", e);
            throw new RuntimeException("Error al obtener el ID del usuario autenticado");
        }
    }
}

