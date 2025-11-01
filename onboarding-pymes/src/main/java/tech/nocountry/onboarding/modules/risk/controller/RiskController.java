package tech.nocountry.onboarding.modules.risk.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tech.nocountry.onboarding.dto.ApiResponse;
import tech.nocountry.onboarding.modules.risk.dto.RiskAssessmentResponse;
import tech.nocountry.onboarding.modules.risk.service.RiskAssessmentService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RiskController {

    private final RiskAssessmentService riskAssessmentService;

    /**
     * Calcular y guardar evaluación de riesgo automática para una solicitud
     */
    @PostMapping("/assess/{applicationId}")
    @PreAuthorize("hasAnyRole('ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<RiskAssessmentResponse>> assessRiskAutomatically(
            @PathVariable String applicationId) {
        
        try {
            log.info("Assessing risk automatically for application: {}", applicationId);
            
            RiskAssessmentResponse response = riskAssessmentService.assessRiskAutomatically(applicationId);
            
            return ResponseEntity.ok(
                ApiResponse.<RiskAssessmentResponse>builder()
                    .success(true)
                    .message("Evaluación de riesgo realizada exitosamente")
                    .data(response)
                    .build()
            );
            
        } catch (RuntimeException e) {
            log.error("Error assessing risk: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<RiskAssessmentResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error assessing risk", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<RiskAssessmentResponse>builder()
                    .success(false)
                    .message("Error al calcular el riesgo: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Calcular y guardar evaluación de riesgo manual por analista
     */
    @PostMapping("/assess/{applicationId}/manual")
    @PreAuthorize("hasAnyRole('ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<RiskAssessmentResponse>> assessRiskManually(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> request) {
        
        try {
            String assessedByUserId = (String) request.get("assessedByUserId");
            Float score = request.get("score") != null ? 
                ((Number) request.get("score")).floatValue() : null;
            String level = (String) request.get("level");
            String comments = (String) request.get("comments");
            
            log.info("Assessing risk manually for application: {} by user: {}", applicationId, assessedByUserId);
            
            RiskAssessmentResponse response = riskAssessmentService.assessRiskManually(
                    applicationId, assessedByUserId, score, level, comments);
            
            return ResponseEntity.ok(
                ApiResponse.<RiskAssessmentResponse>builder()
                    .success(true)
                    .message("Evaluación de riesgo manual realizada exitosamente")
                    .data(response)
                    .build()
            );
            
        } catch (RuntimeException e) {
            log.error("Error assessing risk manually: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<RiskAssessmentResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error assessing risk manually", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<RiskAssessmentResponse>builder()
                    .success(false)
                    .message("Error al calcular el riesgo: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Obtener la evaluación de riesgo más reciente de una solicitud
     */
    @GetMapping("/application/{applicationId}/latest")
    @PreAuthorize("hasAnyRole('APPLICANT', 'ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<RiskAssessmentResponse>> getLatestAssessment(
            @PathVariable String applicationId) {
        
        try {
            log.info("Getting latest risk assessment for application: {}", applicationId);
            
            RiskAssessmentResponse response = riskAssessmentService.getLatestAssessment(applicationId);
            
            return ResponseEntity.ok(
                ApiResponse.<RiskAssessmentResponse>builder()
                    .success(true)
                    .message("Evaluación de riesgo obtenida exitosamente")
                    .data(response)
                    .build()
            );
            
        } catch (RuntimeException e) {
            log.error("Error getting latest assessment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<RiskAssessmentResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Error getting latest assessment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<RiskAssessmentResponse>builder()
                    .success(false)
                    .message("Error al obtener la evaluación: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Obtener todas las evaluaciones de riesgo de una solicitud
     */
    @GetMapping("/application/{applicationId}")
    @PreAuthorize("hasAnyRole('ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<RiskAssessmentResponse>>> getAllAssessments(
            @PathVariable String applicationId) {
        
        try {
            log.info("Getting all risk assessments for application: {}", applicationId);
            
            List<RiskAssessmentResponse> assessments = riskAssessmentService.getAllAssessments(applicationId);
            
            return ResponseEntity.ok(
                ApiResponse.<List<RiskAssessmentResponse>>builder()
                    .success(true)
                    .message("Evaluaciones de riesgo obtenidas exitosamente")
                    .data(assessments)
                    .build()
            );
            
        } catch (Exception e) {
            log.error("Error getting all assessments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<List<RiskAssessmentResponse>>builder()
                    .success(false)
                    .message("Error al obtener las evaluaciones: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Obtener estadísticas de riesgo
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ANALYST', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRiskStatistics() {
        
        try {
            log.info("Getting risk statistics");
            
            Map<String, Object> statistics = riskAssessmentService.getRiskStatistics();
            
            return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("Estadísticas de riesgo obtenidas exitosamente")
                    .data(statistics)
                    .build()
            );
            
        } catch (Exception e) {
            log.error("Error getting risk statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Error al obtener las estadísticas: " + e.getMessage())
                    .build());
        }
    }
}

