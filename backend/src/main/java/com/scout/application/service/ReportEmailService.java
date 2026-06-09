package com.scout.application.service;

import com.scout.application.dto.ReportDetailDto;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportEmailService {

    private final JavaMailSender mailSender;
    private final ReportPdfService reportPdfService;

    @Value("${reports.email.to:}")
    private String reportEmailTo;

    @Value("${reports.email.from:}")
    private String reportEmailFrom;

    public void sendReportPdf(ReportDetailDto report) {
        if (reportEmailTo == null || reportEmailTo.isBlank()) {
            throw new IllegalStateException("REPORT_EMAIL_TO is not configured");
        }
        if (reportEmailFrom == null || reportEmailFrom.isBlank()) {
            throw new IllegalStateException("REPORT_EMAIL_FROM or SMTP_USERNAME is not configured");
        }

        try {
            byte[] pdf = reportPdfService.toPdf(report);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(reportEmailFrom);
            helper.setTo(reportEmailTo);
            helper.setSubject(report.getTitle());
            helper.setText("Attached is your SignalScout report PDF.\n\n" + report.getTitle(), false);
            helper.addAttachment(filename(report), new ByteArrayResource(pdf), "application/pdf");
            mailSender.send(message);
        } catch (MessagingException exception) {
            throw new IllegalStateException("Failed to send report email", exception);
        }
    }

    private String filename(ReportDetailDto report) {
        return report.getTitle()
                .replaceAll("[^A-Za-z0-9._-]+", "-")
                .replaceAll("-+", "-")
                .toLowerCase() + ".pdf";
    }
}
