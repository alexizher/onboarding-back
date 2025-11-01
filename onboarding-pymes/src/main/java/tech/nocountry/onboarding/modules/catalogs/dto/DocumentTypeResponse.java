package tech.nocountry.onboarding.modules.catalogs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTypeResponse {
    private String documentTypeId;
    private String name;
    private String description;
    private Boolean isRequired;
}

