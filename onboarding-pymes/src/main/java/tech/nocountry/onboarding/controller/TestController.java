
package tech.nocountry.onboarding.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.nocountry.onboarding.dto.RoleDTO;
import tech.nocountry.onboarding.entities.Role;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.modules.users.dto.UserResponse;
import tech.nocountry.onboarding.services.RoleService;
import tech.nocountry.onboarding.services.UserService;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {
    
    private final JdbcTemplate jdbcTemplate;
    private final UserService userService;
    private final RoleService roleService;

    public TestController(UserService userService, RoleService roleService,JdbcTemplate jdbcTemplate) {
        this.userService = userService;
        this.roleService = roleService;
        this.jdbcTemplate = jdbcTemplate;
    }
    
    //Verifica que la aplicacion funciona
    @GetMapping
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "La aplicación está funcionando correctamente");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
    
    //Verifica conexion a base de datos
    @GetMapping("/db")
    public ResponseEntity<Map<String, String>> dbCheck() {
        Map<String, String> response = new HashMap<>();
        // Lógica para verificar la conexión a la base de datos
        try {
            // Intentar ejecutar una consulta simple para verificar la conexión
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            response.put("status", "OK");
            response.put("message", "Conexión a la base de datos verificada");
            response.put("database", "Conectado");
        } catch (DataAccessException e) {
            response.put("status", "ERROR");
            response.put("message", "Error en la conexión a la base de datos");
            response.put("database", "Desconectado");
            response.put("error", e.getMessage());
        }
        response.put("status", "OK");
        response.put("message", "Conexión a la base de datos verificada");
        response.put("database", "Conectado");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
    
    //Lista todos los usuarios
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> listUsers() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<User> users = userService.findAllActiveUsers();
            List<UserResponse> userResponses = users.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
            response.put("status", "OK");
            response.put("message", "Usuarios obtenidos correctamente");
            response.put("count", userResponses.size());
            response.put("data", userResponses);
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Error al obtener usuarios");
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    //Lista todos los roles
    @GetMapping("/roles")
    public ResponseEntity<Map<String, Object>> listRoles() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Role> roles = roleService.getAllActiveRoles();
            List<RoleDTO> roleDTOs = roles.stream()
                .map(RoleDTO::new)
                .collect(Collectors.toList());
            response.put("status", "OK");
            response.put("message", "Roles obtenidos correctamente");
            response.put("count", roleDTOs.size());
            response.put("data", roleDTOs);
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Error al obtener roles");
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    // Método helper para convertir User a UserResponse
    private UserResponse mapToResponse(User user) {
        String roleId = null;
        String roleName = null;
        try {
            if (user.getRole() != null) {
                roleId = user.getRole().getRoleId();
                roleName = user.getRole().getName();
            }
        } catch (Exception e) {
            // Log warning si hay error accediendo al rol
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
