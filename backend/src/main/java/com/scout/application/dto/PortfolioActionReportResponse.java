package com.scout.application.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PortfolioActionReportResponse {
    private LocalDateTime generatedAt;
    private BigDecimal totalMarketValueCad;
    private RiskSettingsDto riskSettings;
    private List<PortfolioActionDto> actions;
}
