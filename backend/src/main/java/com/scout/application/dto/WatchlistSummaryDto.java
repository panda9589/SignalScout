package com.scout.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Watchlist summary response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchlistSummaryDto {
    
    private Long companyId;
    private String ticker;
    private String name;
    private String sector;
    
    private Integer latestStockScore;
    private String latestRecommendation;
    
    private Long recentEventCount;
}
