package tech.nocountry.onboarding.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.nocountry.onboarding.entities.TokenBlacklist;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.repositories.TokenBlacklistRepository;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${jwt.secret:mySecretKey123456789012345678901234567890}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 24 horas
    private long expiration;

    @Autowired
    private TokenBlacklistRepository tokenBlacklistRepository;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("username", user.getUsername());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().getRoleId());
        claims.put("permissions", user.getRole().getPermissions());
        
        return createToken(claims, user.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        // Generar JWT ID único para tracking y blacklisting
        String jti = UUID.randomUUID().toString();
        claims.put("jti", jti);
        
        return Jwts.builder()
                .id(jti) // JWT ID para blacklisting
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extraer JWT ID del token
     */
    public String extractJti(String token) {
        try {
            return extractClaim(token, Claims::getId);
        } catch (Exception e) {
            return null;
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, ClaimsResolver<T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.resolve(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token);
            return (extractedUsername != null && 
                   extractedUsername.equals(username) && 
                   !isTokenExpired(token) &&
                   !isTokenBlacklisted(token)); // Verificar blacklist
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Validar token sin verificar username (para casos especiales)
     */
    public Boolean validateToken(String token) {
        try {
            // Verificar si el token está expirado
            if (isTokenExpired(token)) {
                return false;
            }
            
            // Verificar si el token está en la blacklist
            String jti = extractJti(token);
            if (jti != null && tokenBlacklistRepository.existsByJti(jti)) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Agregar token a la blacklist
     */
    public void blacklistToken(String token, String userId, String reason) {
        try {
            String jti = extractJti(token);
            if (jti != null) {
                TokenBlacklist blacklistEntry = TokenBlacklist.builder()
                        .tokenId(UUID.randomUUID().toString())
                        .jti(jti)
                        .userId(userId)
                        .reason(reason)
                        .build();
                
                tokenBlacklistRepository.save(blacklistEntry);
            }
        } catch (Exception e) {
            // Log error pero no fallar
            System.err.println("Error al agregar token a blacklist: " + e.getMessage());
        }
    }

    /**
     * Verificar si un token está en la blacklist
     */
    public boolean isTokenBlacklisted(String token) {
        try {
            String jti = extractJti(token);
            return jti != null && tokenBlacklistRepository.existsByJti(jti);
        } catch (Exception e) {
            return false;
        }
    }

    @FunctionalInterface
    public interface ClaimsResolver<T> {
        T resolve(Claims claims);
    }
}
