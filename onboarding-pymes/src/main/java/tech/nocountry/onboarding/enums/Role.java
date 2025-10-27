package tech.nocountry.onboarding.enums;

/**
 * Roles específicos para la plataforma de onboarding de créditos para PYMES.
 * Incluye identificador técnico (roleId), nombre legible y descripción.
 */
public enum Role {

    APPLICANT("ROLE_APPLICANT", "Solicitante", "PYME que solicita crédito"),
    ANALYST("ROLE_ANALYST", "Analista", "Analista que revisa solicitudes"),
    MANAGER("ROLE_MANAGER", "Gerente", "Gerente que aprueba solicitudes"),
    ADMIN("ROLE_ADMIN", "Administrador", "Administrador del sistema");

    private final String roleId;
    private final String name;
    private final String description;

    Role(String roleId, String name, String description) {
        this.roleId = roleId;
        this.name = name;
        this.description = description;
    }

    public String getRoleId() {
        return roleId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static Role fromRoleId(String roleId) {
        for (Role role : values()) {
            if (role.roleId.equals(roleId)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Role no encontrado: " + roleId);
    }
}


