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
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private LocalDateTime lastLogin;
    
    // Métodos estáticos para crear respuestas comunes
    public static AuthResponse success(String message, Long userId, String username, String email, String fullName) {
        return AuthResponse.builder()
                .success(true)
                .message(message)
                .userId(userId)
                .username(username)
                .email(email)
                .fullName(fullName)
                .build();
    }
    
    public static AuthResponse error(String message) {
        return AuthResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
