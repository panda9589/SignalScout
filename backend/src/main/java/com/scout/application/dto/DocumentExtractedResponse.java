package com.scout.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response from manual document paste endpoint
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentExtractedResponse {
    
    private Long documentId;
    private Long eventId;
    
    private String ticker;
    private String companyName;
    
    private String eventType;
    private String summary;
    private String whatChanged;
    
    private Integer bullishScore;
    private Integer bearishScore;
    private Integer sourceQualityScore;
    private Integer confidenceScore;
    
    private Integer stockScore;
    private String recommendation;
    
    private List<String> bullCase;
    private List<String> bearCase;
    private List<String> risks;
    private List<String> watchItems;
    
    private Boolean requiresManualReview;
    private String manualReviewReason;
    
    private LocalDateTime createdAt;
}
