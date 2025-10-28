package tech.nocountry.onboarding.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.repositories.UserRepository;
import tech.nocountry.onboarding.services.SecurityAuditService;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityAuditService securityAuditService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, 
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            try {
                String username = jwtService.extractUsername(token);
                
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    Optional<User> userOptional = userRepository.findByUsername(username);
                    
                    if (userOptional.isPresent() && jwtService.validateToken(token, username)) {
                        User user = userOptional.get();
                        
                        // Verificar que el usuario esté activo
                        if (!user.getIsActive()) {
                            securityAuditService.logSecurityEvent(
                                user.getUserId(), 
                                "AUTHENTICATION_BLOCKED", 
                                ipAddress, 
                                userAgent, 
                                "Intento de autenticación con cuenta desactivada", 
                                "HIGH"
                            );
                            filterChain.doFilter(request, response);
                            return;
                        }
                        
                        UsernamePasswordAuthenticationToken authToken = 
                            new UsernamePasswordAuthenticationToken(
                                user, 
                                null, 
                                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().getRoleId()))
                            );
                        
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        
                        // Log de autenticación exitosa
                        securityAuditService.logSecurityEvent(
                            user.getUserId(), 
                            "AUTHENTICATION_SUCCESS", 
                            ipAddress, 
                            userAgent, 
                            "Autenticación exitosa via JWT", 
                            "LOW"
                        );
                    } else {
                        // Log de token inválido
                        securityAuditService.logSecurityEvent(
                            null, 
                            "AUTHENTICATION_FAILED", 
                            ipAddress, 
                            userAgent, 
                            "Token JWT inválido o usuario no encontrado", 
                            "MEDIUM"
                        );
                    }
                }
            } catch (Exception e) {
                // Log de error en validación de token
                securityAuditService.logSecurityEvent(
                    null, 
                    "AUTHENTICATION_ERROR", 
                    ipAddress, 
                    userAgent, 
                    "Error en validación de token JWT: " + e.getMessage(), 
                    "MEDIUM"
                );
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}