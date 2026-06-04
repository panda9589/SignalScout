package com.scout.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Company DTO for API responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyDto {
    
    private Long id;
    private String ticker;
    private String name;
    private String sector;
    private String industry;
    private String cik;
    private String exchange;
    private String country;
    private String currency;
}
