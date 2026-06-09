package com.scout.presentation.controller;

import com.scout.application.dto.ReportDetailDto;
import com.scout.application.dto.UpcomingEventDto;
import com.scout.application.service.ReportEmailService;
import com.scout.application.service.ReportPdfService;
import com.scout.application.service.ReportService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ReportControllerTest {

    private final ReportService reportService = mock(ReportService.class);
    private final ReportPdfService reportPdfService = mock(ReportPdfService.class);
    private final ReportEmailService reportEmailService = mock(ReportEmailService.class);
    private final ReportController controller = new ReportController(reportService, reportPdfService, reportEmailService);

    @Test
    void generateReportReturnsCreatedReport() {
        ReportDetailDto report = ReportDetailDto.builder()
                .id(1L)
                .reportType("weekly")
                .title("Weekly Market Intelligence Report")
                .reportMarkdown("# Weekly Market Intelligence Report")
                .createdAt(LocalDateTime.now())
                .build();
        when(reportService.generateReport("weekly")).thenReturn(report);

        var response = controller.generateReport("weekly");

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(report);
    }

    @Test
    void exportPdfReturnsAttachment() {
        ReportDetailDto report = ReportDetailDto.builder()
                .id(1L)
                .reportType("weekly")
                .title("Weekly Market Intelligence Report")
                .reportMarkdown("# Weekly Market Intelligence Report")
                .createdAt(LocalDateTime.now())
                .build();
        when(reportService.getReport(1L)).thenReturn(report);
        when(reportPdfService.toPdf(report)).thenReturn(new byte[]{1, 2, 3});

        var response = controller.exportPdf(1L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("attachment");
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/pdf");
        assertThat(response.getBody()).containsExactly(1, 2, 3);
    }

    @Test
    void emailReportSendsPdfAndReturnsAccepted() {
        ReportDetailDto report = ReportDetailDto.builder()
                .id(1L)
                .reportType("weekly")
                .title("Weekly Market Intelligence Report")
                .reportMarkdown("# Weekly Market Intelligence Report")
                .createdAt(LocalDateTime.now())
                .build();
        when(reportService.getReport(1L)).thenReturn(report);

        var response = controller.emailReport(1L);

        assertThat(response.getStatusCode().value()).isEqualTo(202);
        verify(reportEmailService).sendReportPdf(report);
    }

    @Test
    void upcomingEventsReturnsWorkflowItems() {
        UpcomingEventDto event = UpcomingEventDto.builder()
                .eventType("WATCHLIST_REVIEW")
                .dueDate(LocalDate.now().plusDays(3))
                .ticker("NVDA")
                .title("Refresh watchlist evidence")
                .priority("medium")
                .build();
        when(reportService.upcomingEvents(5)).thenReturn(List.of(event));

        var response = controller.upcomingEvents(5);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsExactly(event);
    }
}
