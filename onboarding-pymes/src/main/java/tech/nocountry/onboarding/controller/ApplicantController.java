package tech.nocountry.onboarding.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.security.JwtService;
import tech.nocountry.onboarding.services.UserService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/applicant")
@CrossOrigin(origins = "*")
public class ApplicantController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @GetMapping("/profile")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String token) {
        try {
            // Extract user ID from JWT token
            String cleanToken = token.replace("Bearer ", "");
            String userId = jwtService.extractUserId(cleanToken);
            
            // Find user by ID
            Optional<User> userOptional = userService.findById(userId);
            
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                // Return user profile information (excluding sensitive data)
                Map<String, Object> profileData = new HashMap<>();
                profileData.put("userId", user.getUserId());
                profileData.put("username", user.getUsername());
                profileData.put("email", user.getEmail());
                profileData.put("fullName", user.getFullName());
                profileData.put("dni", user.getDni());
                profileData.put("phone", user.getPhone());
                profileData.put("isActive", user.getIsActive());
                profileData.put("createdAt", user.getCreatedAt());
                profileData.put("lastLogin", user.getLastLogin());
                
                return ResponseEntity.ok().body(profileData);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error retrieving profile: " + e.getMessage());
        }
    }

    @PostMapping("/submit-application")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<?> submitApplication(@RequestBody Object application) {
        // Lógica para enviar solicitud de crédito
        return ResponseEntity.ok("Solicitud enviada");
    }
}