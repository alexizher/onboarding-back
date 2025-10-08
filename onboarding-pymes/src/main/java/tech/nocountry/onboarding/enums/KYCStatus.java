package tech.nocountry.onboarding.enums;

public enum KYCStatus {
    PENDING("Pendiente"),
    IN_PROGRESS("En Proceso"),
    VERIFIED("Verificado"),
    REJECTED("Rechazado"),
    EXPIRED("Expirado");

    private final String description;

    KYCStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
