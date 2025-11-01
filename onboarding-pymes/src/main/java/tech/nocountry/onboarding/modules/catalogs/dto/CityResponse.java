package tech.nocountry.onboarding.modules.catalogs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CityResponse {
    private String cityId;
    private String departmentId;
    private String departmentName;
    private String name;
    private String code;
}

