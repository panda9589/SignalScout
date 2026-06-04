package com.scout.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.time.LocalDate;
import java.util.List;

/**
 * Extraction output from OpenAI Structured Outputs API
 * Must match the schema defined in docs/EXTRACTION.md
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractionOutputDto {
    
    @NotBlank
    @JsonProperty("schema_version")
    private String schemaVersion;
    
    private String ticker;

    @JsonProperty("company_name")
    private String companyName;

    @JsonProperty("document_relevance")
    private String documentRelevance;
    
    @NotBlank
    @JsonProperty("event_type")
    private String eventType;
    
    @JsonProperty("event_date")
    private LocalDate eventDate;
    
    @NotBlank
    private String summary;
    
    @JsonProperty("what_changed")
    private String whatChanged;
    
    private List<EvidenceDto> evidence;

    @JsonProperty("bull_case")
    private List<String> bullCase;

    @JsonProperty("bear_case")
    private List<String> bearCase;

    private List<String> risks;

    @JsonProperty("watch_items")
    private List<String> watchItems;
    
    @Min(0) @Max(100)
    @JsonProperty("source_quality_score")
    private Integer sourceQualityScore;
    
    @Min(0) @Max(100)
    @JsonProperty("bullish_score")
    private Integer bullishScore;
    
    @Min(0) @Max(100)
    @JsonProperty("bearish_score")
    private Integer bearishScore;
    
    @Min(0) @Max(100)
    @JsonProperty("confidence_score")
    private Integer confidenceScore;
    
    @JsonProperty("requires_manual_review")
    private Boolean requiresManualReview;

    @JsonProperty("manual_review_reason")
    private String manualReviewReason;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvidenceDto {
        private String text;
        private String location;

        @JsonProperty("evidence_type")
        private String evidenceType;
    }
}
