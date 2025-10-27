package tech.nocountry.onboarding.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tech.nocountry.onboarding.services.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/analyst")
@CrossOrigin(origins = "*")
public class AnalystController {

    @Autowired
    private UserService userService;

    @GetMapping("/applications")
    @PreAuthorize("hasAnyRole('ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> getApplications() {
        // Obtener todos los usuarios activos (solicitantes) para revisión
        List<tech.nocountry.onboarding.entities.User> activeUsers = userService.findAllActiveUsers();
        return ResponseEntity.ok(activeUsers);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> getUserDetails(@PathVariable String userId) {
        // Obtener detalles de un usuario específico para análisis
        return userService.findById(userId)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/assign-task")
    @PreAuthorize("hasAnyRole('ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> assignTask(@RequestBody Object task) {
        // Lógica para asignar tareas
        return ResponseEntity.ok("Tarea asignada");
    }
}