package tech.nocountry.onboarding.modules.applications.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tech.nocountry.onboarding.dto.ApiResponse;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.modules.applications.dto.ApplicationRequest;
import tech.nocountry.onboarding.modules.applications.dto.ApplicationResponse;
import tech.nocountry.onboarding.modules.applications.dto.ApplicationUpdateRequest;
import tech.nocountry.onboarding.modules.applications.service.ApplicationService;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CreditApplicationController {

    private final ApplicationService applicationService;

    /**
     * Crear una nueva solicitud de crédito
     */
    @PostMapping
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> createApplication(
            @Valid @RequestBody ApplicationRequest request) {
        
        try {
            String userId = getCurrentUserId();
            log.info("Creating application for user: {}", userId);
            
            ApplicationResponse response = applicationService.createApplication(userId, request);
            
            return ResponseEntity.ok(
                ApiResponse.<ApplicationResponse>builder()
                    .success(true)
                    .message("Solicitud de crédito creada exitosamente")
                    .data(response)
                    .build()
            );
            
        } catch (Exception e) {
            log.error("Error creating application", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<ApplicationResponse>builder()
                    .success(false)
                    .message("Error al crear la solicitud: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Obtener una solicitud por ID
     */
    @GetMapping("/{applicationId}")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> getApplication(
            @PathVariable String applicationId) {
        
        try {
            log.info("Getting application: {}", applicationId);
            
            ApplicationResponse response = applicationService.getApplicationById(applicationId);
            
            return ResponseEntity.ok(
                ApiResponse.<ApplicationResponse>builder()
                    .success(true)
                    .message("Solicitud obtenida exitosamente")
                    .data(response)
                    .build()
            );
            
        } catch (RuntimeException e) {
            log.error("Application not found: {}", applicationId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<ApplicationResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error getting application", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<ApplicationResponse>builder()
                    .success(false)
                    .message("Error al obtener la solicitud: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Obtener todas las solicitudes del usuario actual
     */
    @GetMapping("/my-applications")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getMyApplications() {
        
        try {
            String userId = getCurrentUserId();
            log.info("Getting applications for user: {}", userId);
            
            List<ApplicationResponse> applications = applicationService.getUserApplications(userId);
            
            return ResponseEntity.ok(
                ApiResponse.<List<ApplicationResponse>>builder()
                    .success(true)
                    .message("Solicitudes obtenidas exitosamente")
                    .data(applications)
                    .build()
            );
            
        } catch (Exception e) {
            log.error("Error getting user applications", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<List<ApplicationResponse>>builder()
                    .success(false)
                    .message("Error al obtener las solicitudes: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Obtener todas las solicitudes por estado (solo para analistas y admins)
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getApplicationsByStatus(
            @PathVariable String status) {
        
        try {
            log.info("Getting applications by status: {}", status);
            
            List<ApplicationResponse> applications = applicationService.getAllApplicationsByStatus(status);
            
            return ResponseEntity.ok(
                ApiResponse.<List<ApplicationResponse>>builder()
                    .success(true)
                    .message("Solicitudes obtenidas exitosamente")
                    .data(applications)
                    .build()
            );
            
        } catch (Exception e) {
            log.error("Error getting applications by status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<List<ApplicationResponse>>builder()
                    .success(false)
                    .message("Error al obtener las solicitudes: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Actualizar una solicitud
     */
    @PutMapping("/{applicationId}")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateApplication(
            @PathVariable String applicationId,
            @Valid @RequestBody ApplicationUpdateRequest request) {
        
        try {
            log.info("Updating application: {}", applicationId);
            
            ApplicationResponse response = applicationService.updateApplication(applicationId, request);
            
            return ResponseEntity.ok(
                ApiResponse.<ApplicationResponse>builder()
                    .success(true)
                    .message("Solicitud actualizada exitosamente")
                    .data(response)
                    .build()
            );
            
        } catch (RuntimeException e) {
            log.error("Application not found: {}", applicationId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<ApplicationResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error updating application", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<ApplicationResponse>builder()
                    .success(false)
                    .message("Error al actualizar la solicitud: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Eliminar una solicitud
     */
    @DeleteMapping("/{applicationId}")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteApplication(
            @PathVariable String applicationId) {
        
        try {
            log.info("Deleting application: {}", applicationId);
            
            applicationService.deleteApplication(applicationId);
            
            return ResponseEntity.ok(
                ApiResponse.<String>builder()
                    .success(true)
                    .message("Solicitud eliminada exitosamente")
                    .data("Solicitud eliminada: " + applicationId)
                    .build()
            );
            
        } catch (RuntimeException e) {
            log.error("Application not found: {}", applicationId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<String>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error deleting application", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<String>builder()
                    .success(false)
                    .message("Error al eliminar la solicitud: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Obtener el ID del usuario actual desde el SecurityContext
     */
    private String getCurrentUserId() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            
            if (principal instanceof User) {
                return ((User) principal).getUserId();
            }
            
            throw new IllegalStateException("Usuario no autenticado o formato de autenticación no válido");
        } catch (Exception e) {
            log.error("Error al obtener el ID del usuario actual: {}", e.getMessage());
            throw new IllegalStateException("No se pudo obtener el ID del usuario: " + e.getMessage());
        }
    }
}

