package tech.nocountry.onboarding.modules.risk.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.nocountry.onboarding.entities.BusinessCategory;
import tech.nocountry.onboarding.entities.CreditApplication;
import tech.nocountry.onboarding.entities.Document;
import tech.nocountry.onboarding.enums.RiskLevel;
import tech.nocountry.onboarding.modules.risk.model.RiskFactors;
import tech.nocountry.onboarding.modules.risk.model.RiskScore;
import tech.nocountry.onboarding.repositories.DocumentRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class RiskCalculator {

    private final DocumentRepository documentRepository;
    private final ObjectMapper objectMapper;

    public RiskCalculator(DocumentRepository documentRepository, ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Calcula el score de riesgo para una solicitud de crédito
     * Utiliza la escala DataCrédito Colombia: 150-950
     * 
     * Para Personas Naturales: Escala 150-950
     * Para PYMEs (Score Acierta): Escala 150-950
     * 
     * Rangos de clasificación:
     * - 700-950: BUENO (Bajo riesgo)
     * - 600-699: REGULAR (Riesgo medio-bajo)
     * - 400-599: REGULAR (Riesgo medio-alto)
     * - 300-399: MALO (Alto riesgo)
     * - 150-299: MUY MALO (Muy alto riesgo)
     * 
     * Nota: Actualmente el sistema está configurado para PYMEs.
     * Los campos company_name y cuit son obligatorios.
     */
    public RiskScore calculateRiskScore(CreditApplication application) {
        log.info("Calculating risk score for application: {}", application.getApplicationId());

        RiskFactors factors = calculateRiskFactors(application);
        Float totalScore = calculateTotalScore(factors);
        String level = determineRiskLevel(totalScore);
        String recommendation = generateRecommendation(totalScore, level);

        log.info("Risk score calculated: {} ({})", totalScore, level);

        return RiskScore.builder()
                .totalScore(totalScore)
                .level(level)
                .factors(factors)
                .recommendation(recommendation)
                .build();
    }

    /**
     * Calcula los factores de riesgo individuales
     */
    private RiskFactors calculateRiskFactors(CreditApplication application) {
        BigDecimal monthlyIncome = application.getMonthlyIncome() != null 
                ? application.getMonthlyIncome() 
                : BigDecimal.ZERO;
        BigDecimal monthlyExpenses = application.getMonthlyExpenses() != null 
                ? application.getMonthlyExpenses() 
                : BigDecimal.ZERO;
        BigDecimal existingDebt = application.getExistingDebt() != null 
                ? application.getExistingDebt() 
                : BigDecimal.ZERO;
        BigDecimal amountRequested = application.getAmountRequested() != null 
                ? application.getAmountRequested() 
                : BigDecimal.ZERO;

        // 1. Debt-to-Income Ratio (DTI): Deuda mensual / Ingresos mensuales
        Float debtToIncomeRatio = 0.0f;
        if (monthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalMonthlyDebt = existingDebt.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            debtToIncomeRatio = totalMonthlyDebt
                    .divide(monthlyIncome, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .floatValue();
        }

        // 2. Amount-to-Income Ratio: Monto solicitado / Ingresos anuales
        Float amountToIncomeRatio = 0.0f;
        if (monthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal annualIncome = monthlyIncome.multiply(BigDecimal.valueOf(12));
            amountToIncomeRatio = amountRequested
                    .divide(annualIncome, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .floatValue();
        }

        // 3. Expense-to-Income Ratio: Gastos / Ingresos
        Float expenseToIncomeRatio = 0.0f;
        if (monthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
            expenseToIncomeRatio = monthlyExpenses
                    .divide(monthlyIncome, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .floatValue();
        }

        // 4. Income Stability Score (0-100): Basado en ingreso reportado
        Float incomeStabilityScore = calculateIncomeStabilityScore(monthlyIncome);

        // 5. Credit History Score (placeholder: 50 por defecto si no hay historial)
        Float creditHistoryScore = 50.0f; // Por defecto, se puede mejorar con integración externa

        // 6. Business Category Risk (0-100): Basado en categoría de negocio
        Float businessCategoryRisk = calculateBusinessCategoryRisk(application.getCategory());

        // 7. Document Completeness: Porcentaje de documentos requeridos
        Float documentCompleteness = calculateDocumentCompleteness(application.getApplicationId());

        // Detalles adicionales
        Map<String, Object> detailsMap = new HashMap<>();
        detailsMap.put("monthlyIncome", monthlyIncome);
        detailsMap.put("monthlyExpenses", monthlyExpenses);
        detailsMap.put("amountRequested", amountRequested);
        detailsMap.put("existingDebt", existingDebt);
        detailsMap.put("creditMonths", application.getCreditMonths());

        String detailsJson = "";
        try {
            detailsJson = objectMapper.writeValueAsString(detailsMap);
        } catch (Exception e) {
            log.warn("Error serializing risk details to JSON: {}", e.getMessage());
        }

        return RiskFactors.builder()
                .debtToIncomeRatio(debtToIncomeRatio)
                .amountToIncomeRatio(amountToIncomeRatio)
                .incomeStabilityScore(incomeStabilityScore)
                .creditHistoryScore(creditHistoryScore)
                .businessCategoryRisk(businessCategoryRisk)
                .documentCompleteness(documentCompleteness)
                .expenseToIncomeRatio(expenseToIncomeRatio)
                .details(detailsJson)
                .build();
    }

    /**
     * Calcula el score total basado en los factores (escala DataCrédito Colombia: 150-950)
     * Actualmente configurado para PYMEs (Score Acierta)
     */
    private Float calculateTotalScore(RiskFactors factors) {
        // Score base en escala DataCrédito Colombia (550 es el punto medio entre 150-950)
        float score = 550.0f;

        // Ajustes basados en DTI (Deuda/Ingresos)
        // Ideal: < 30%, Aceptable: 30-40%, Riesgoso: > 40%
        if (factors.getDebtToIncomeRatio() > 40) {
            score -= 200; // Muy alto riesgo
        } else if (factors.getDebtToIncomeRatio() > 30) {
            score -= 100; // Alto riesgo
        } else if (factors.getDebtToIncomeRatio() > 20) {
            score -= 50; // Moderado riesgo
        } else {
            score += 50; // Bajo riesgo
        }

        // Ajustes basados en Amount-to-Income (Monto/Ingresos anuales)
        // Ideal: < 50%, Aceptable: 50-100%, Riesgoso: > 100%
        if (factors.getAmountToIncomeRatio() > 100) {
            score -= 150; // Muy alto riesgo
        } else if (factors.getAmountToIncomeRatio() > 50) {
            score -= 50; // Alto riesgo
        } else {
            score += 50; // Bajo riesgo
        }

        // Ajustes basados en Expense-to-Income
        // Ideal: < 60%, Aceptable: 60-80%, Riesgoso: > 80%
        if (factors.getExpenseToIncomeRatio() > 80) {
            score -= 100; // Muy alto riesgo
        } else if (factors.getExpenseToIncomeRatio() > 60) {
            score -= 50; // Alto riesgo
        }

        // Ajustes basados en estabilidad de ingresos (0-100 -> se convierte a escala 150-950)
        // Si incomeStabilityScore es 80, contribuye +280 puntos
        float incomeContribution = (factors.getIncomeStabilityScore() / 100.0f) * 280f;
        score += (incomeContribution - 140f); // Ajuste centrado

        // Ajustes basados en historial crediticio (0-100 -> se convierte a escala 150-950)
        // Si creditHistoryScore es 80, contribuye +140 puntos
        float creditContribution = (factors.getCreditHistoryScore() / 100.0f) * 140f;
        score += (creditContribution - 70f); // Ajuste centrado

        // Ajustes basados en categoría de negocio (inversa: riesgo alto = score bajo)
        // Si businessCategoryRisk es 20 (bajo riesgo), suma puntos
        float categoryContribution = ((100 - factors.getBusinessCategoryRisk()) / 100.0f) * 140f;
        score += (categoryContribution - 70f); // Ajuste centrado

        // Ajustes basados en completitud de documentos (0-100 -> se convierte a escala 150-950)
        // Si documentCompleteness es 80, contribuye +100 puntos
        float documentContribution = (factors.getDocumentCompleteness() / 100.0f) * 100f;
        score += (documentContribution - 50f); // Ajuste centrado

        // Normalizar entre 150 y 950 (escala DataCrédito Colombia para Personas Naturales y PYMEs)
        return Math.max(150, Math.min(950, score));
    }

    /**
     * Determina el nivel de riesgo basado en el score (escala DataCrédito Colombia: 150-950)
     * 700-950: Bajo riesgo (BUENO)
     * 600-699: Riesgo medio-bajo (REGULAR)
     * 400-599: Riesgo medio-alto (REGULAR)
     * 300-399: Alto riesgo (MALO)
     * 150-299: Muy alto riesgo (MUY MALO)
     */
    private String determineRiskLevel(Float score) {
        if (score >= 700) {
            return RiskLevel.LOW.name(); // BUENO
        } else if (score >= 600) {
            return RiskLevel.MEDIUM.name(); // REGULAR (medio-bajo)
        } else if (score >= 400) {
            return RiskLevel.MEDIUM.name(); // REGULAR (medio-alto)
        } else if (score >= 300) {
            return RiskLevel.HIGH.name(); // MALO
        } else {
            return RiskLevel.VERY_HIGH.name(); // MUY MALO (150-299)
        }
    }

    /**
     * Genera una recomendación basada en el score (escala DataCrédito Colombia: 150-950)
     * Nota: Actualmente configurado para PYMEs (Score Acierta)
     */
    private String generateRecommendation(Float score, String level) {
        if (RiskLevel.LOW.name().equals(level)) {
            return String.format("Score DataCrédito (Acierta PYMEs): %.0f (BUENO, rango 700-950). Riesgo bajo. Solicitud aprobable con condiciones estándar.", score);
        } else if (RiskLevel.MEDIUM.name().equals(level)) {
            if (score >= 600) {
                return String.format("Score DataCrédito (Acierta PYMEs): %.0f (REGULAR - medio-bajo, rango 600-699). Riesgo moderado. Requiere revisión adicional y posiblemente garantías.", score);
            } else {
                return String.format("Score DataCrédito (Acierta PYMEs): %.0f (REGULAR - medio-alto, rango 400-599). Riesgo medio. Requiere revisión adicional y garantías.", score);
            }
        } else if (RiskLevel.HIGH.name().equals(level)) {
            return String.format("Score DataCrédito (Acierta PYMEs): %.0f (MALO, rango 300-399). Riesgo alto. Se recomienda revisión exhaustiva y garantías adicionales.", score);
        } else {
            return String.format("Score DataCrédito (Acierta PYMEs): %.0f (MUY MALO, rango 150-299). Riesgo muy alto. Se recomienda rechazar o solicitar condiciones muy restrictivas.", score);
        }
    }

    /**
     * Calcula score de estabilidad de ingresos (0-100)
     */
    private Float calculateIncomeStabilityScore(BigDecimal monthlyIncome) {
        if (monthlyIncome == null || monthlyIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0f;
        }
        // Score basado en el nivel de ingresos reportado
        // Ingresos altos (> $100k anuales) = mayor estabilidad
        BigDecimal annualIncome = monthlyIncome.multiply(BigDecimal.valueOf(12));
        if (annualIncome.compareTo(BigDecimal.valueOf(100000)) >= 0) {
            return 80.0f;
        } else if (annualIncome.compareTo(BigDecimal.valueOf(50000)) >= 0) {
            return 60.0f;
        } else if (annualIncome.compareTo(BigDecimal.valueOf(25000)) >= 0) {
            return 40.0f;
        } else {
            return 20.0f;
        }
    }

    /**
     * Calcula riesgo por categoría de negocio (0-100, donde 100 es mayor riesgo)
     */
    private Float calculateBusinessCategoryRisk(BusinessCategory category) {
        if (category == null) {
            return 50.0f; // Neutral si no hay categoría
        }
        // Se puede usar el campo risk_level de BusinessCategory si existe
        // Por ahora, retornamos un valor por defecto
        String riskLevel = category.getRiskLevel(); // Puede ser null
        if (riskLevel != null) {
            switch (riskLevel.toUpperCase()) {
                case "LOW":
                    return 20.0f;
                case "MEDIUM":
                    return 50.0f;
                case "HIGH":
                    return 80.0f;
                default:
                    return 50.0f;
            }
        }
        return 50.0f; // Neutral por defecto
    }

    /**
     * Calcula completitud de documentos (0-100)
     */
    private Float calculateDocumentCompleteness(String applicationId) {
        try {
            List<Document> documents = documentRepository.findByApplicationId(applicationId);
            long verifiedCount = documents.stream()
                    .filter(doc -> "verified".equals(doc.getVerificationStatus()))
                    .count();
            long totalCount = documents.size();
            
            if (totalCount == 0) {
                return 0.0f; // Sin documentos
            }
            
            // Score basado en documentos verificados
            return (float) (verifiedCount * 100.0 / Math.max(totalCount, 5)); // Asumiendo 5 docs requeridos
        } catch (Exception e) {
            log.warn("Error calculating document completeness: {}", e.getMessage());
            return 50.0f; // Neutral si hay error
        }
    }
}

