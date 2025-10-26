package tech.nocountry.onboarding.modules.documents.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadRequest {

    @NotBlank(message = "El ID de la solicitud es obligatorio")
    private String applicationId;

    @NotBlank(message = "El tipo de documento es obligatorio")
    private String documentTypeId;

    @NotBlank(message = "El nombre del archivo es obligatorio")
    private String fileName;

    @NotBlank(message = "El contenido del archivo es obligatorio")
    private String fileContent;

    private String mimeType;
}

