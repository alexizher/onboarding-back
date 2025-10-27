package tech.nocountry.onboarding.modules.applications.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponse {

    private String applicationId;
    private String userId;
    private String status;
    
    // Información de la empresa
    private String companyName;
    private String cuit;
    private String companyAddress;
    
    // Información del crédito
    private BigDecimal amountRequested;
    private String purpose;
    private Integer creditMonths;
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpenses;
    private BigDecimal existingDebt;
    
    // Relaciones
    private String categoryId;
    private String professionId;
    private String destinationId;
    private String stateId;
    private String cityId;
    private String assignedTo;
    
    // Términos
    private Boolean acceptTerms;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

