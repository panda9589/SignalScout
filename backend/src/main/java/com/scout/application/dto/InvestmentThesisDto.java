package com.scout.application.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvestmentThesisDto {
    private Long id;
    private Long companyId;
    private String ticker;
    private String companyName;
    private String thesisText;
    private String buyReason;
    private Integer expectedTimeHorizonMonths;
    private String status;
}
