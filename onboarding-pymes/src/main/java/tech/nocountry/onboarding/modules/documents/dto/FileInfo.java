package tech.nocountry.onboarding.modules.documents.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {
    private String fileName;
    private byte[] fileBytes;
    private String mimeType;
}

