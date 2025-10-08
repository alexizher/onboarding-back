package tech.nocountry.onboarding.enums;

public enum RiskLevel {
    LOW("Bajo"),
    MEDIUM("Medio"),
    HIGH("Alto"),
    VERY_HIGH("Muy Alto");

    private final String description;

    RiskLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
