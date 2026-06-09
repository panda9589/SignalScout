package com.scout.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReportDetailDto {
    private Long id;
    private String reportType;
    private String title;
    private String reportMarkdown;
    private String reportJson;
    private LocalDateTime createdAt;
}
