package tech.nocountry.onboarding.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import tech.nocountry.onboarding.services.AuthService;

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
    public Map<String, Object> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");
        
        if (email == null || password == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Email y password son requeridos");
            return response;
        }
        
        return authService.loginUser(email, password);
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
}
