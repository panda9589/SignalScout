package com.scout.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class UpcomingEventDto {
    private String eventType;
    private LocalDate dueDate;
    private String ticker;
    private String companyName;
    private String title;
    private String reason;
    private String priority;
}
