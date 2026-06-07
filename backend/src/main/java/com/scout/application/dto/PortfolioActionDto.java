package com.scout.application.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PortfolioActionDto {
    private Long companyId;
    private String ticker;
    private String companyName;
    private String sector;
    private String action;
    private Integer stockScore;
    private Integer sourceQualityScore;
    private String latestRecommendation;
    private BigDecimal currentWeightPct;
    private BigDecimal sectorExposurePct;
    private boolean hasHolding;
    private boolean hasActiveThesis;
    private String reason;
    private String buyTrigger;
    private String sellTrigger;
    private String riskNotes;
}
