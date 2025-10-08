package tech.nocountry.onboarding.enums;

public enum DocumentType {
    IDENTIFICATION("Identificación"),
    TAX_CERTIFICATE("Certificado Tributario"),
    BANK_STATEMENT("Estado de Cuenta Bancario"),
    FINANCIAL_STATEMENT("Estados Financieros"),
    BUSINESS_LICENSE("Licencia de Funcionamiento"),
    COMMERCIAL_REGISTRY("Registro Mercantil"),
    INCOME_PROOF("Comprobante de Ingresos"),
    PROPERTY_DEED("Escritura de Propiedad"),
    GUARANTEE_DOCUMENT("Documento de Garantía"),
    OTHER("Otro");

    private final String description;

    DocumentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
