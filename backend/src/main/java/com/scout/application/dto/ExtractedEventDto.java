package com.scout.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Extracted event DTO for API responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractedEventDto {
    
    private Long id;
    private String eventType;
    private LocalDate eventDate;
    private String summary;
    private String whatChanged;
    
    private Integer bullishScore;
    private Integer bearishScore;
    private Integer sourceQualityScore;
    private Integer confidenceScore;
    
    private List<String> bullCase;
    private List<String> bearCase;
    private List<String> risks;
    private List<String> watchItems;
    
    private Boolean requiresManualReview;
    private String manualReviewReason;
    
    private LocalDateTime createdAt;
}
