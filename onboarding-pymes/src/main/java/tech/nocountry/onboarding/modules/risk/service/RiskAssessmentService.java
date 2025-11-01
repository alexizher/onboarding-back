package tech.nocountry.onboarding.modules.risk.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.nocountry.onboarding.entities.CreditApplication;
import tech.nocountry.onboarding.entities.RiskAssessment;
import tech.nocountry.onboarding.entities.User;
import tech.nocountry.onboarding.modules.risk.dto.RiskAssessmentResponse;
import tech.nocountry.onboarding.modules.risk.model.RiskScore;
import tech.nocountry.onboarding.repositories.CreditApplicationRepository;
import tech.nocountry.onboarding.repositories.RiskAssessmentRepository;
import tech.nocountry.onboarding.repositories.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RiskAssessmentService {

    private final RiskAssessmentRepository riskAssessmentRepository;
    private final CreditApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final RiskCalculator riskCalculator;
    private final ObjectMapper objectMapper;

    /**
     * Calcula y guarda una evaluación de riesgo automática para una solicitud
     */
    @Transactional
    public RiskAssessmentResponse assessRiskAutomatically(String applicationId) {
        log.info("Assessing risk automatically for application: {}", applicationId);

        CreditApplication application = applicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        // Calcular score de riesgo
        RiskScore riskScore = riskCalculator.calculateRiskScore(application);

        // Guardar evaluación
        RiskAssessment assessment = RiskAssessment.builder()
                .application(application)
                .score(riskScore.getTotalScore())
                .level(riskScore.getLevel())
                .details(convertFactorsToJson(riskScore.getFactors()))
                .isAutomated(true)
                .assessedAt(java.time.LocalDateTime.now())
                .build();

        RiskAssessment saved = riskAssessmentRepository.save(assessment);
        log.info("Risk assessment saved: {} (score: {}, level: {})", 
                 saved.getAssessmentId(), saved.getScore(), saved.getLevel());

        return mapToResponse(saved, riskScore.getRecommendation());
    }

    /**
     * Calcula y guarda una evaluación de riesgo manual por un analista
     */
    @Transactional
    public RiskAssessmentResponse assessRiskManually(String applicationId, String assessedByUserId, 
                                                     Float score, String level, String comments) {
        log.info("Assessing risk manually for application: {} by user: {}", applicationId, assessedByUserId);

        CreditApplication application = applicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        User assessor = userRepository.findByUserId(assessedByUserId)
                .orElseThrow(() -> new RuntimeException("Usuario evaluador no encontrado"));

        // Si no se proporciona score, calcularlo automáticamente
        if (score == null) {
            RiskScore riskScore = riskCalculator.calculateRiskScore(application);
            score = riskScore.getTotalScore();
            if (level == null || level.isBlank()) {
                level = riskScore.getLevel();
            }
        }

        // Si no se proporciona level, determinarlo del score (escala DataCrédito 150-950)
        if (level == null || level.isBlank()) {
            if (score >= 700) {
                level = "LOW"; // BUENO (700-950)
            } else if (score >= 600) {
                level = "MEDIUM"; // REGULAR (medio-bajo, 600-699)
            } else if (score >= 400) {
                level = "MEDIUM"; // REGULAR (medio-alto, 400-599)
            } else if (score >= 300) {
                level = "HIGH"; // MALO (300-399)
            } else {
                level = "VERY_HIGH"; // MUY MALO (150-299)
            }
        }

        // Guardar evaluación
        RiskAssessment assessment = RiskAssessment.builder()
                .application(application)
                .score(score)
                .level(level)
                .details(comments != null ? comments : "Evaluación manual")
                .assessedBy(assessor)
                .isAutomated(false)
                .assessedAt(java.time.LocalDateTime.now())
                .build();

        RiskAssessment saved = riskAssessmentRepository.save(assessment);
        log.info("Manual risk assessment saved: {} (score: {}, level: {})", 
                 saved.getAssessmentId(), saved.getScore(), saved.getLevel());

        String recommendation = generateRecommendationFromLevel(level);
        return mapToResponse(saved, recommendation);
    }

    /**
     * Obtiene la evaluación de riesgo más reciente de una solicitud
     */
    @Transactional(readOnly = true)
    public RiskAssessmentResponse getLatestAssessment(String applicationId) {
        log.info("Getting latest risk assessment for application: {}", applicationId);

        RiskAssessment assessment = riskAssessmentRepository.findLatestByApplicationId(applicationId)
                .orElseThrow(() -> new RuntimeException("No se encontró evaluación de riesgo para esta solicitud"));

        String recommendation = generateRecommendationFromLevel(assessment.getLevel());
        return mapToResponse(assessment, recommendation);
    }

    /**
     * Obtiene todas las evaluaciones de riesgo de una solicitud
     */
    @Transactional(readOnly = true)
    public List<RiskAssessmentResponse> getAllAssessments(String applicationId) {
        log.info("Getting all risk assessments for application: {}", applicationId);

        List<RiskAssessment> assessments = riskAssessmentRepository.findByApplicationId(applicationId);
        return assessments.stream()
                .map(assessment -> {
                    String recommendation = generateRecommendationFromLevel(assessment.getLevel());
                    return mapToResponse(assessment, recommendation);
                })
                .collect(Collectors.toList());
    }

    /**
     * Obtiene estadísticas de riesgo
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getRiskStatistics() {
        log.info("Getting risk statistics");

        java.util.Map<String, Object> stats = new java.util.HashMap<>();

        long total = riskAssessmentRepository.count();
        long low = riskAssessmentRepository.countByLevel("LOW");
        long medium = riskAssessmentRepository.countByLevel("MEDIUM");
        long high = riskAssessmentRepository.countByLevel("HIGH");
        long veryHigh = riskAssessmentRepository.countByLevel("VERY_HIGH");

        Double avgScore = riskAssessmentRepository.getAverageScore();

        stats.put("total", total);
        stats.put("low", low);
        stats.put("medium", medium);
        stats.put("high", high);
        stats.put("veryHigh", veryHigh);
        stats.put("averageScore", avgScore != null ? avgScore : 0.0);

        if (total > 0) {
            stats.put("lowPercentage", (double) low / total * 100);
            stats.put("mediumPercentage", (double) medium / total * 100);
            stats.put("highPercentage", (double) high / total * 100);
            stats.put("veryHighPercentage", (double) veryHigh / total * 100);
        }

        return stats;
    }

    private RiskAssessmentResponse mapToResponse(RiskAssessment assessment, String recommendation) {
        String applicationId = null;
        try {
            if (assessment.getApplication() != null) {
                applicationId = assessment.getApplication().getApplicationId();
            }
        } catch (Exception e) {
            log.warn("Error accessing application: {}", e.getMessage());
        }

        String assessedBy = null;
        try {
            if (assessment.getAssessedBy() != null) {
                assessedBy = assessment.getAssessedBy().getUserId();
            }
        } catch (Exception e) {
            log.warn("Error accessing assessedBy: {}", e.getMessage());
        }

        return RiskAssessmentResponse.builder()
                .assessmentId(assessment.getAssessmentId())
                .applicationId(applicationId)
                .score(assessment.getScore())
                .level(assessment.getLevel())
                .details(assessment.getDetails())
                .assessedBy(assessedBy)
                .assessedAt(assessment.getAssessedAt())
                .isAutomated(assessment.getIsAutomated())
                .recommendation(recommendation)
                .build();
    }

    private String convertFactorsToJson(tech.nocountry.onboarding.modules.risk.model.RiskFactors factors) {
        try {
            return objectMapper.writeValueAsString(factors);
        } catch (Exception e) {
            log.warn("Error converting factors to JSON: {}", e.getMessage());
            return "";
        }
    }

    private String generateRecommendationFromLevel(String level) {
        // Nota: Actualmente configurado para PYMEs (Score Acierta)
        // Para Personas Naturales se usa la misma escala pero con modelo diferente
        if ("LOW".equals(level)) {
            return "Score DataCrédito (Acierta PYMEs): BUENO (700-950). Riesgo bajo. Solicitud aprobable con condiciones estándar.";
        } else if ("MEDIUM".equals(level)) {
            return "Score DataCrédito (Acierta PYMEs): REGULAR (400-699). Riesgo medio. Requiere revisión adicional y posiblemente garantías.";
        } else if ("HIGH".equals(level)) {
            return "Score DataCrédito (Acierta PYMEs): MALO (300-399). Riesgo alto. Se recomienda revisión exhaustiva y garantías adicionales.";
        } else {
            return "Score DataCrédito (Acierta PYMEs): MUY MALO (150-299). Riesgo muy alto. Se recomienda rechazar o solicitar condiciones muy restrictivas.";
        }
    }
}

