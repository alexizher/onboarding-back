package tech.nocountry.onboarding.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.nocountry.onboarding.entities.RefreshToken;
import tech.nocountry.onboarding.repositories.RefreshTokenRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class RefreshTokenService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration:1800000}") // 30 minutos por defecto
    private long refreshTokenExpiration;

    /**
     * Generar refresh token (duración corta para sistema bancario)
     */
    public RefreshToken generateRefreshToken(String userId, String ipAddress, String userAgent, String deviceId) {
        // Generar token seguro (32 bytes)
        SecureRandom random = new SecureRandom();
        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        // Hash del token para almacenar
        String tokenHash = hashToken(token);

        // Expiración corta (30 minutos) para sistema bancario
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000);

        RefreshToken refreshToken = RefreshToken.builder()
                .refreshTokenId(UUID.randomUUID().toString())
                .userId(userId)
                .token(token)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .isRevoked(false)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .deviceId(deviceId)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Validar refresh token
     */
    public Optional<RefreshToken> validateRefreshToken(String token) {
        String tokenHash = hashToken(token);
        Optional<RefreshToken> tokenOptional = refreshTokenRepository.findByTokenHash(tokenHash);

        if (tokenOptional.isEmpty()) {
            return Optional.empty();
        }

        RefreshToken refreshToken = tokenOptional.get();

        // Verificar que no esté revocado
        if (refreshToken.getIsRevoked() != null && refreshToken.getIsRevoked()) {
            return Optional.empty();
        }

        // Verificar que no esté expirado
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }

        return Optional.of(refreshToken);
    }

    /**
     * Revocar refresh token
     */
    public void revokeRefreshToken(String token) {
        String tokenHash = hashToken(token);
        refreshTokenRepository.revokeToken(tokenHash, LocalDateTime.now());
    }

    /**
     * Revocar todos los refresh tokens de un usuario
     */
    public void revokeAllUserTokens(String userId) {
        refreshTokenRepository.revokeAllUserTokens(userId, LocalDateTime.now());
    }

    /**
     * Obtener refresh tokens activos de un usuario
     */
    public List<RefreshToken> getUserActiveTokens(String userId) {
        return refreshTokenRepository.findActiveTokensByUserId(userId, LocalDateTime.now());
    }

    /**
     * Generar hash SHA-256 de un token
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar hash de token", e);
        }
    }

    /**
     * Limpiar tokens expirados (ejecutar periódicamente)
     */
    @Scheduled(fixedRate = 3600000) // Cada hora
    public void cleanupExpiredTokens() {
        List<RefreshToken> expiredTokens = refreshTokenRepository.findExpiredTokens(LocalDateTime.now());
        for (RefreshToken token : expiredTokens) {
            token.setIsRevoked(true);
        }
        refreshTokenRepository.saveAll(expiredTokens);
    }
}

