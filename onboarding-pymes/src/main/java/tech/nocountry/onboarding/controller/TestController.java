package tech.nocountry.onboarding.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.repositories.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/bcrypt-test")
    public Map<String, Object> testBcrypt(@RequestBody Map<String, String> request) {
        String password = request.get("password");
        String email = request.get("email");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Test 1: Verificar longitud de contraseña
            response.put("passwordLength", password != null ? password.length() : 0);
            response.put("passwordBytes", password != null ? password.getBytes().length : 0);
            
            // Test 2: Buscar usuario
            Optional<User> userOptional = userRepository.findByEmail(email);
            response.put("userFound", userOptional.isPresent());
            
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                response.put("hashLength", user.getPasswordHash().length());
                response.put("hash", user.getPasswordHash());
                
                // Test 3: Verificar contraseña
                try {
                    boolean matches = passwordEncoder.matches(password, user.getPasswordHash());
                    response.put("passwordMatches", matches);
                    response.put("bcryptTestSuccess", true);
                } catch (Exception e) {
                    response.put("bcryptError", e.getMessage());
                    response.put("bcryptTestSuccess", false);
                }
            }
            
            response.put("success", true);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return response;
    }
}
