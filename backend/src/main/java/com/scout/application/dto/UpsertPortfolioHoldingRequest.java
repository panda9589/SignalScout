package com.scout.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpsertPortfolioHoldingRequest {
    @NotNull(message = "accountId is required")
    private Long accountId;

    private Long companyId;

    @NotBlank(message = "symbol is required")
    private String symbol;

    private BigDecimal quantity;
    private BigDecimal avgCost;
    private BigDecimal marketValueCad;
}
