package com.scout.application.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PortfolioAccountDto {
    private Long id;
    private String accountName;
    private String accountType;
    private String baseCurrency;
}
