package com.scout.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpsertInvestmentThesisRequest {
    @NotNull(message = "companyId is required")
    private Long companyId;

    @NotBlank(message = "thesisText is required")
    private String thesisText;

    private String buyReason;
    private Integer expectedTimeHorizonMonths;
    private String status = "active";
}
