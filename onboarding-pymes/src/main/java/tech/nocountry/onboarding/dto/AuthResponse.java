package tech.nocountry.onboarding.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    
    private boolean success;
    private String message;
    private String userId;
    private String username;
    private String email;
    private String fullName;
    private String token;
    private LocalDateTime lastLogin;
    
    // Métodos estáticos para crear respuestas comunes
    public static AuthResponse success(String message, String userId, String username, String email, String fullName, String token) {
        return AuthResponse.builder()
                .success(true)
                .message(message)
                .userId(userId)
                .username(username)
                .email(email)
                .fullName(fullName)
                .token(token)
                .build();
    }
    
    public static AuthResponse error(String message) {
        return AuthResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
