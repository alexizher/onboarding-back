package tech.nocountry.onboarding.modules.applications.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationFilterRequest {
    
    // Filtros
    private String status;
    private String userId;  // Usuario que creó la solicitud
    private String assignedToUserId;  // Usuario asignado (analista)
    private String companyName;
    private String cuit;
    
    // Filtros de fecha
    private LocalDateTime createdFrom;
    private LocalDateTime createdTo;
    private LocalDateTime updatedFrom;
    private LocalDateTime updatedTo;
    
    // Filtros de monto
    private java.math.BigDecimal minAmount;
    private java.math.BigDecimal maxAmount;
    
    // Paginación
    @Builder.Default
    private Integer page = 0;
    @Builder.Default
    private Integer size = 20;
    
    // Ordenamiento
    @Builder.Default
    private String sortBy = "createdAt";  // createdAt, updatedAt, companyName, amountRequested
    @Builder.Default
    private String sortDirection = "DESC";  // ASC, DESC
}

