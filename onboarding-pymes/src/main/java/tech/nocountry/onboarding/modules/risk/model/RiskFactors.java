package tech.nocountry.onboarding.modules.risk.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskFactors {
    private Float debtToIncomeRatio; // Deuda/Ingresos
    private Float amountToIncomeRatio; // Monto solicitado/Ingresos mensuales
    private Float incomeStabilityScore; // Estabilidad de ingresos
    private Float creditHistoryScore; // Historial crediticio (si está disponible)
    private Float businessCategoryRisk; // Riesgo por categoría de negocio
    private Float documentCompleteness; // Completitud de documentos
    private Float expenseToIncomeRatio; // Gastos/Ingresos
    private String details; // Detalles adicionales en JSON
}

