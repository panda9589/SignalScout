package com.scout.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "reports.scheduler.enabled", havingValue = "true")
public class ScheduledReportService {

    private final ReportService reportService;
    private final ReportEmailService reportEmailService;

    @Scheduled(cron = "${reports.scheduler.daily-cron:0 0 18 * * MON-FRI}")
    public void generateDailyDigest() {
        log.info("Starting scheduled daily digest generation");
        reportEmailService.sendReportPdf(reportService.generateReport("daily"));
    }

    @Scheduled(cron = "${reports.scheduler.weekly-cron:0 0 18 * * SUN}")
    public void generateWeeklyReport() {
        log.info("Starting scheduled weekly report generation");
        reportEmailService.sendReportPdf(reportService.generateReport("weekly"));
    }
}
