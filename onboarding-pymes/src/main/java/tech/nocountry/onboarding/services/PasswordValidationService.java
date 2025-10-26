package tech.nocountry.onboarding.services;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class PasswordValidationService {

    // Patrones de validación
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]");
    
    // Contraseñas comunes débiles
    private static final String[] COMMON_PASSWORDS = {
        "password", "123456", "123456789", "qwerty", "abc123", "password123",
        "admin", "letmein", "welcome", "monkey", "1234567890", "dragon",
        "master", "hello", "freedom", "whatever", "qazwsx", "trustno1"
    };

    /**
     * Validar fortaleza de contraseña
     */
    public PasswordValidationResult validatePassword(String password) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Verificar longitud mínima
        if (password == null || password.length() < 8) {
            errors.add("La contraseña debe tener al menos 8 caracteres");
        }

        // Verificar longitud máxima
        if (password != null && password.length() > 128) {
            errors.add("La contraseña no puede tener más de 128 caracteres");
        }

        if (password != null) {
            // Verificar mayúsculas
            if (!UPPERCASE_PATTERN.matcher(password).find()) {
                errors.add("La contraseña debe contener al menos una letra mayúscula");
            }

            // Verificar minúsculas
            if (!LOWERCASE_PATTERN.matcher(password).find()) {
                errors.add("La contraseña debe contener al menos una letra minúscula");
            }

            // Verificar dígitos
            if (!DIGIT_PATTERN.matcher(password).find()) {
                errors.add("La contraseña debe contener al menos un número");
            }

            // Verificar caracteres especiales
            if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
                warnings.add("Se recomienda incluir al menos un carácter especial (!@#$%^&*)");
            }

            // Verificar contraseñas comunes
            if (isCommonPassword(password)) {
                errors.add("La contraseña es demasiado común y fácil de adivinar");
            }

            // Verificar patrones repetitivos
            if (hasRepeatingPatterns(password)) {
                warnings.add("La contraseña contiene patrones repetitivos que la hacen menos segura");
            }

            // Verificar secuencias
            if (hasSequentialCharacters(password)) {
                warnings.add("La contraseña contiene secuencias que la hacen menos segura");
            }
        }

        boolean isValid = errors.isEmpty();
        int strength = calculatePasswordStrength(password);

        return new PasswordValidationResult(isValid, errors, warnings, strength);
    }

    /**
     * Verificar si es una contraseña común
     */
    private boolean isCommonPassword(String password) {
        if (password == null) return false;
        
        String lowerPassword = password.toLowerCase();
        for (String common : COMMON_PASSWORDS) {
            if (lowerPassword.contains(common.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verificar patrones repetitivos
     */
    private boolean hasRepeatingPatterns(String password) {
        if (password == null || password.length() < 4) return false;
        
        // Verificar caracteres repetidos consecutivos
        for (int i = 0; i < password.length() - 2; i++) {
            if (password.charAt(i) == password.charAt(i + 1) && 
                password.charAt(i + 1) == password.charAt(i + 2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verificar secuencias de caracteres
     */
    private boolean hasSequentialCharacters(String password) {
        if (password == null || password.length() < 3) return false;
        
        String lowerPassword = password.toLowerCase();
        
        // Verificar secuencias alfabéticas
        for (int i = 0; i < lowerPassword.length() - 2; i++) {
            char c1 = lowerPassword.charAt(i);
            char c2 = lowerPassword.charAt(i + 1);
            char c3 = lowerPassword.charAt(i + 2);
            
            if (c2 == c1 + 1 && c3 == c2 + 1) {
                return true;
            }
        }
        
        // Verificar secuencias numéricas
        for (int i = 0; i < password.length() - 2; i++) {
            char c1 = password.charAt(i);
            char c2 = password.charAt(i + 1);
            char c3 = password.charAt(i + 2);
            
            if (Character.isDigit(c1) && Character.isDigit(c2) && Character.isDigit(c3)) {
                if (c2 == c1 + 1 && c3 == c2 + 1) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Calcular fortaleza de la contraseña (0-100)
     */
    private int calculatePasswordStrength(String password) {
        if (password == null) return 0;
        
        int score = 0;
        
        // Puntos por longitud
        if (password.length() >= 8) score += 10;
        if (password.length() >= 12) score += 10;
        if (password.length() >= 16) score += 10;
        
        // Puntos por complejidad
        if (UPPERCASE_PATTERN.matcher(password).find()) score += 10;
        if (LOWERCASE_PATTERN.matcher(password).find()) score += 10;
        if (DIGIT_PATTERN.matcher(password).find()) score += 10;
        if (SPECIAL_CHAR_PATTERN.matcher(password).find()) score += 15;
        
        // Puntos por variedad de caracteres
        long uniqueChars = password.chars().distinct().count();
        if (uniqueChars >= 8) score += 10;
        if (uniqueChars >= 12) score += 5;
        
        // Penalizaciones
        if (isCommonPassword(password)) score -= 30;
        if (hasRepeatingPatterns(password)) score -= 15;
        if (hasSequentialCharacters(password)) score -= 10;
        
        return Math.max(0, Math.min(100, score));
    }

    /**
     * Clase para el resultado de validación
     */
    public static class PasswordValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;
        private final int strength;

        public PasswordValidationResult(boolean valid, List<String> errors, List<String> warnings, int strength) {
            this.valid = valid;
            this.errors = errors;
            this.warnings = warnings;
            this.strength = strength;
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        public int getStrength() { return strength; }
        
        public String getStrengthLevel() {
            if (strength >= 80) return "MUY_FUERTE";
            if (strength >= 60) return "FUERTE";
            if (strength >= 40) return "MEDIA";
            if (strength >= 20) return "DÉBIL";
            return "MUY_DÉBIL";
        }
    }
}
