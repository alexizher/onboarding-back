package tech.nocountry.onboarding.enums;

public enum ApplicationStatus {
    DRAFT("Borrador"),
    SUBMITTED("Enviada"),
    UNDER_REVIEW("En Revisión"),
    DOCUMENTS_PENDING("Documentos Pendientes"),
    APPROVED("Aprobada"),
    REJECTED("Rechazada"),
    CANCELLED("Cancelada");

    private final String description;

    ApplicationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
