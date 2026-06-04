package com.scout.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

/**
 * Request to manually paste a document for extraction
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualDocumentPasteRequest {
    
    @NotBlank(message = "raw_text is required")
    private String rawText;
    
    @NotBlank(message = "company_id is required")
    private String companyId;
    
    @NotBlank(message = "source_type is required")
    private String sourceType;
}
