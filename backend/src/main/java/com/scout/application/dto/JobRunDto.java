package com.scout.application.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class JobRunDto {
    private Long id;
    private String jobName;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String status;
    private String errorMessage;
    private Integer documentsFound;
    private Integer documentsProcessed;
    private Integer aiCalls;
    private BigDecimal estimatedCostUsd;
}
