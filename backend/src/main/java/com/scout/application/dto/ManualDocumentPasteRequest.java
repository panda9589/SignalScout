package com.scout.application.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    
    @JsonProperty("raw_text")
    @JsonAlias("rawText")
    @NotBlank(message = "raw_text is required")
    private String rawText;
    
    @JsonProperty("company_id")
    @JsonAlias("companyId")
    @NotBlank(message = "company_id is required")
    private String companyId;
    
    @JsonProperty("source_type")
    @JsonAlias("sourceType")
    @NotBlank(message = "source_type is required")
    private String sourceType;
}
