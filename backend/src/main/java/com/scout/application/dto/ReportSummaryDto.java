package com.scout.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReportSummaryDto {
    private Long id;
    private String reportType;
    private String title;
    private LocalDateTime createdAt;
}
