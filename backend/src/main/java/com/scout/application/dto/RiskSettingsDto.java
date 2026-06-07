package com.scout.application.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RiskSettingsDto {
    private BigDecimal maxSingleStockPositionPct;
    private BigDecimal maxSectorExposurePct;
    private Integer minScoreForNewBuy;
    private Integer minSourceQualityForNewBuy;
}
