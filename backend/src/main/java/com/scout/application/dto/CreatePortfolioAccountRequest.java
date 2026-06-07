package com.scout.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePortfolioAccountRequest {
    @NotBlank(message = "accountName is required")
    private String accountName;

    @NotBlank(message = "accountType is required")
    private String accountType;

    private String baseCurrency = "CAD";
}
