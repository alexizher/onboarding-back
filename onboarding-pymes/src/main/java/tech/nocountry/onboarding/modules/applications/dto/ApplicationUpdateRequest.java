package tech.nocountry.onboarding.modules.applications.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationUpdateRequest {

    @Size(max = 255, message = "El nombre de la empresa no puede exceder 255 caracteres")
    private String companyName;

    @Size(max = 20, message = "El CUIT no puede exceder 20 caracteres")
    private String cuit;

    @Size(max = 255, message = "La dirección no puede exceder 255 caracteres")
    private String companyAddress;

    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    @DecimalMax(value = "999999999999.99", message = "El monto excede el máximo permitido")
    private BigDecimal amountRequested;

    @Size(max = 500, message = "El propósito no puede exceder 500 caracteres")
    private String purpose;

    @Min(value = 1, message = "Los meses de crédito deben ser al menos 1")
    @Max(value = 120, message = "Los meses de crédito no pueden exceder 120")
    private Integer creditMonths;

    @DecimalMin(value = "0.00", message = "El ingreso mensual debe ser mayor o igual a cero")
    private BigDecimal monthlyIncome;

    @DecimalMin(value = "0.00", message = "Los gastos mensuales deben ser mayor o igual a cero")
    private BigDecimal monthlyExpenses;

    @DecimalMin(value = "0.00", message = "Las deudas existentes deben ser mayor o igual a cero")
    private BigDecimal existingDebt;

    private String status;
    private String categoryId;
    private String professionId;
    private String destinationId;
    private String stateId;
    private String cityId;
    private String assignedTo;
}

