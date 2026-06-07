package com.scout.application.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PortfolioHoldingDto {
    private Long id;
    private Long accountId;
    private String accountName;
    private Long companyId;
    private String ticker;
    private String companyName;
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal avgCost;
    private BigDecimal marketValueCad;
    private BigDecimal portfolioWeight;
}
