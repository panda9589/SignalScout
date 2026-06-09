package com.scout.presentation.controller;

import com.scout.application.dto.ReportDetailDto;
import com.scout.application.dto.ReportSummaryDto;
import com.scout.application.dto.UpcomingEventDto;
import com.scout.application.service.ReportEmailService;
import com.scout.application.service.ReportPdfService;
import com.scout.application.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ReportPdfService reportPdfService;
    private final ReportEmailService reportEmailService;

    @GetMapping
    public ResponseEntity<List<ReportSummaryDto>> listReports(
            @RequestParam(required = false) String reportType,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(reportService.listReports(reportType, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReportDetailDto> getReport(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.getReport(id));
    }

    @PostMapping("/generate")
    public ResponseEntity<ReportDetailDto> generateReport(
            @RequestParam(defaultValue = "weekly") String reportType) {
        return ResponseEntity.status(201).body(reportService.generateReport(reportType));
    }

    @GetMapping("/{id}/markdown")
    public ResponseEntity<String> exportMarkdown(@PathVariable Long id) {
        ReportDetailDto report = reportService.getReport(id);
        String filename = report.getTitle()
                .replaceAll("[^A-Za-z0-9._-]+", "-")
                .replaceAll("-+", "-")
                .toLowerCase() + ".md";

        return ResponseEntity.ok()
                .contentType(new MediaType("text", "markdown", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(report.getReportMarkdown());
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        ReportDetailDto report = reportService.getReport(id);
        String filename = filename(report, "pdf");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(reportPdfService.toPdf(report));
    }

    @PostMapping("/{id}/email")
    public ResponseEntity<Void> emailReport(@PathVariable Long id) {
        reportEmailService.sendReportPdf(reportService.getReport(id));
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/upcoming-events")
    public ResponseEntity<List<UpcomingEventDto>> upcomingEvents(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(reportService.upcomingEvents(limit));
    }

    private String filename(ReportDetailDto report, String extension) {
        return report.getTitle()
                .replaceAll("[^A-Za-z0-9._-]+", "-")
                .replaceAll("-+", "-")
                .toLowerCase() + "." + extension;
    }
}
