package tech.nocountry.onboarding.enums;

public enum CompanyType {
    SOLE_PROPRIETORSHIP("Persona Humana"),
    PARTNERSHIP("Sociedad en Comandita"),
    CORPORATION("Corporación - Sociedad Anónima"),
    LLC("Sociedad de Responsabilidad Limitada"),
    COOPERATIVE("Cooperativa"),
    NON_PROFIT("Sin Fines de Lucro");

    private final String description;

    CompanyType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
