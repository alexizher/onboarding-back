package tech.nocountry.onboarding.modules.risk.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskScore {
    private Float totalScore; // Score total en escala DataCrédito Colombia (150-950) - Para Personas Naturales y PYMEs (Score Acierta)
    private String level; // LOW, MEDIUM, HIGH, VERY_HIGH
    private RiskFactors factors; // Factores que contribuyeron al score
    private String recommendation; // Recomendación basada en el score
}

