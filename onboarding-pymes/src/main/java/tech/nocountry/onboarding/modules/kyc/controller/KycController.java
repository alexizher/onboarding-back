package tech.nocountry.onboarding.modules.kyc.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tech.nocountry.onboarding.dto.ApiResponse;
import tech.nocountry.onboarding.modules.kyc.dto.*;
import tech.nocountry.onboarding.modules.kyc.service.KycService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class KycController {

    private final KycService kycService;

    /**
     * Iniciar verificación KYC para una solicitud
     */
    @PostMapping("/verify")
    @PreAuthorize("hasAnyRole('ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<KycVerificationResponse>> initiateVerification(
            @Valid @RequestBody KycVerificationRequest request) {
        
        try {
            log.info("Initiating KYC verification for application: {}, type: {}", 
                     request.getApplicationId(), request.getVerificationType());
            
            KycVerificationResponse response = kycService.initiateVerification(
                    request.getApplicationId(),
                    request.getVerificationType() != null ? request.getVerificationType() : "IDENTITY",
                    request.getProvider()
            );
            
            return ResponseEntity.ok(
                ApiResponse.<KycVerificationResponse>builder()
                    .success(true)
                    .message("Verificación KYC iniciada exitosamente")
                    .data(response)
                    .build()
            );
            
        } catch (RuntimeException e) {
            log.error("Error initiating KYC verification: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<KycVerificationResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error initiating KYC verification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<KycVerificationResponse>builder()
                    .success(false)
                    .message("Error al iniciar la verificación KYC: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Obtener todas las verificaciones de una solicitud
     */
    @GetMapping("/application/{applicationId}")
    @PreAuthorize("hasAnyRole('ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<KycVerificationResponse>>> getVerificationsByApplication(
            @PathVariable String applicationId) {
        
        try {
            log.info("Getting KYC verifications for application: {}", applicationId);
            
            List<KycVerificationResponse> verifications = kycService.getVerificationsByApplication(applicationId);
            
            return ResponseEntity.ok(
                ApiResponse.<List<KycVerificationResponse>>builder()
                    .success(true)
                    .message("Verificaciones KYC obtenidas exitosamente")
                    .data(verifications)
                    .build()
            );
            
        } catch (Exception e) {
            log.error("Error getting KYC verifications", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<List<KycVerificationResponse>>builder()
                    .success(false)
                    .message("Error al obtener las verificaciones: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Obtener todas las verificaciones de un usuario
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<KycVerificationResponse>>> getVerificationsByUser(
            @PathVariable String userId) {
        
        try {
            log.info("Getting KYC verifications for user: {}", userId);
            
            List<KycVerificationResponse> verifications = kycService.getVerificationsByUser(userId);
            
            return ResponseEntity.ok(
                ApiResponse.<List<KycVerificationResponse>>builder()
                    .success(true)
                    .message("Verificaciones KYC obtenidas exitosamente")
                    .data(verifications)
                    .build()
            );
            
        } catch (Exception e) {
            log.error("Error getting KYC verifications by user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<List<KycVerificationResponse>>builder()
                    .success(false)
                    .message("Error al obtener las verificaciones: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Obtener la verificación más reciente de un tipo específico para una solicitud
     */
    @GetMapping("/application/{applicationId}/latest")
    @PreAuthorize("hasAnyRole('ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<KycVerificationResponse>> getLatestVerification(
            @PathVariable String applicationId,
            @RequestParam(defaultValue = "IDENTITY") String verificationType) {
        
        try {
            log.info("Getting latest KYC verification for application: {}, type: {}", applicationId, verificationType);
            
            KycVerificationResponse response = kycService.getLatestVerification(applicationId, verificationType);
            
            return ResponseEntity.ok(
                ApiResponse.<KycVerificationResponse>builder()
                    .success(true)
                    .message("Verificación KYC obtenida exitosamente")
                    .data(response)
                    .build()
            );
            
        } catch (RuntimeException e) {
            log.error("Error getting latest KYC verification: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<KycVerificationResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error getting latest KYC verification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<KycVerificationResponse>builder()
                    .success(false)
                    .message("Error al obtener la verificación: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Obtener estadísticas de verificaciones KYC
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getKycStatistics() {
        
        try {
            log.info("Getting KYC statistics");
            
            Map<String, Object> statistics = kycService.getKycStatistics();
            
            return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("Estadísticas KYC obtenidas exitosamente")
                    .data(statistics)
                    .build()
            );
            
        } catch (Exception e) {
            log.error("Error getting KYC statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Error al obtener las estadísticas: " + e.getMessage())
                    .build());
        }
    }
}

