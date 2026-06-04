package com.scout.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Company detail response with latest score and events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyDetailResponse {
    
    private CompanyDto company;
    private Integer latestStockScore;
    private String latestRecommendation;
    private List<ExtractedEventDto> recentEvents;
}
