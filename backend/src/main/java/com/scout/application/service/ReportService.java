package com.scout.application.service;

import com.scout.application.dto.PortfolioActionDto;
import com.scout.application.dto.PortfolioActionReportResponse;
import com.scout.application.dto.ReportDetailDto;
import com.scout.application.dto.ReportSummaryDto;
import com.scout.application.dto.UpcomingEventDto;
import com.scout.domain.entity.JobRun;
import com.scout.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String DEFAULT_USER_EMAIL = "local@signalscout.dev";

    private final JdbcTemplate jdbcTemplate;
    private final JobRunService jobRunService;
    private final PortfolioService portfolioService;

    @Transactional(readOnly = true)
    public List<ReportSummaryDto> listReports(String reportType, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        if (reportType == null || reportType.isBlank()) {
            return jdbcTemplate.query("""
                    select id, report_type, title, created_at
                    from reports
                    order by created_at desc
                    limit ?
                    """, this::mapSummary, safeLimit);
        }

        return jdbcTemplate.query("""
                select id, report_type, title, created_at
                from reports
                where report_type = ?
                order by created_at desc
                limit ?
                """, this::mapSummary, normalizeReportType(reportType), safeLimit);
    }

    @Transactional(readOnly = true)
    public ReportDetailDto getReport(Long id) {
        return jdbcTemplate.query("""
                select id, report_type, title, report_markdown, report_json, created_at
                from reports
                where id = ?
                """, this::mapDetail, id).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<UpcomingEventDto> upcomingEvents(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<UpcomingEventDto> manualReviews = jdbcTemplate.query("""
                select c.ticker, c.name as company_name, e.summary, e.manual_review_reason, e.created_at
                from extracted_events e
                left join companies c on c.id = e.company_id
                where e.requires_manual_review = true
                order by e.created_at desc
                limit ?
                """, (rs, rowNum) -> UpcomingEventDto.builder()
                .eventType("MANUAL_REVIEW")
                .dueDate(LocalDate.now().plusDays(1))
                .ticker(rs.getString("ticker"))
                .companyName(rs.getString("company_name"))
                .title("Manual review needed")
                .reason(nonBlank(rs.getString("manual_review_reason"), rs.getString("summary")))
                .priority("high")
                .build(), safeLimit);

        if (manualReviews.size() >= safeLimit) {
            return manualReviews;
        }

        List<UpcomingEventDto> staleReviews = jdbcTemplate.query("""
                select c.ticker, c.name as company_name, max(e.created_at) as latest_signal_at
                from companies c
                left join extracted_events e on e.company_id = c.id
                group by c.id, c.ticker, c.name
                having max(e.created_at) is null or max(e.created_at) < now() - interval '14 days'
                order by max(e.created_at) nulls first, c.ticker
                limit ?
                """, (rs, rowNum) -> UpcomingEventDto.builder()
                .eventType("WATCHLIST_REVIEW")
                .dueDate(LocalDate.now().plusDays(3))
                .ticker(rs.getString("ticker"))
                .companyName(rs.getString("company_name"))
                .title("Refresh watchlist evidence")
                .reason("No recent extracted event in the last 14 days.")
                .priority("medium")
                .build(), safeLimit - manualReviews.size());

        return java.util.stream.Stream.concat(manualReviews.stream(), staleReviews.stream()).toList();
    }

    @Transactional
    public ReportDetailDto generateReport(String requestedType) {
        String reportType = normalizeReportType(requestedType);
        int lookbackDays = "weekly".equals(reportType) ? 7 : 1;
        JobRun jobRun = jobRunService.start(reportJobName(reportType), 1);
        try {
            Long userId = ensureDefaultUser();
            PortfolioActionReportResponse actionReport = portfolioService.generateActionReport();
            String markdown = buildMarkdown(reportType, lookbackDays, actionReport);
            String title = reportTitle(reportType);
            Long reportId = jdbcTemplate.queryForObject("""
                    insert into reports (user_id, report_type, title, report_markdown, report_json)
                    values (?, ?, ?, ?, ?::jsonb)
                    returning id
                    """, Long.class, userId, reportType, title, markdown, reportJson(actionReport));
            jobRunService.complete(jobRun, 1);
            return getReport(reportId);
        } catch (Exception exception) {
            jobRunService.fail(jobRun, exception);
            throw exception;
        }
    }

    private String buildMarkdown(
            String reportType,
            int lookbackDays,
            PortfolioActionReportResponse actionReport) {

        List<SignalRow> signals = recentSignals(lookbackDays, 25);
        List<SignalRow> positive = signals.stream()
                .filter(signal -> signal.stockScore() != null && signal.stockScore() >= 70)
                .limit(5)
                .toList();
        List<SignalRow> negative = signals.stream()
                .filter(signal -> signal.stockScore() != null && signal.stockScore() < 45)
                .limit(5)
                .toList();
        List<SignalRow> manualChecks = signals.stream()
                .filter(signal -> signal.requiresManualReview()
                        || nullToZero(signal.sourceQualityScore()) < 60
                        || nullToZero(signal.confidenceScore()) < 55)
                .limit(8)
                .toList();
        List<PortfolioActionDto> materialActions = actionReport.getActions().stream()
                .filter(action -> !"HOLD".equals(action.getAction()))
                .sorted(Comparator.comparing(PortfolioActionDto::getAction))
                .toList();
        List<UpcomingEventDto> upcoming = upcomingEvents(8);

        StringBuilder markdown = new StringBuilder();
        markdown.append("# ").append(reportTitle(reportType)).append("\n\n");
        markdown.append("Generated: ").append(LocalDateTime.now()).append("\n\n");
        markdown.append("## Executive Summary\n\n");
        markdown.append("- Recent extracted signals: ").append(signals.size()).append("\n");
        markdown.append("- Portfolio value tracked: ").append(actionReport.getTotalMarketValueCad()).append(" CAD\n");
        markdown.append("- Non-hold portfolio actions: ").append(materialActions.size()).append("\n");
        markdown.append("- Manual checks: ").append(manualChecks.size()).append("\n\n");

        markdown.append("## Biggest Company-Specific Changes\n\n");
        appendSignals(markdown, signals.stream().limit(8).toList(), "No recent extracted company changes.");

        markdown.append("## Strongest Positive Signals\n\n");
        appendSignals(markdown, positive, "No strong positive signals in this period.");

        markdown.append("## Strongest Negative Signals\n\n");
        appendSignals(markdown, negative, "No strong negative signals in this period.");

        markdown.append("## Watchlist Updates\n\n");
        appendSignals(markdown, signals.stream()
                .filter(signal -> signal.whatChanged() != null && !signal.whatChanged().isBlank())
                .limit(8)
                .toList(), "No explicit watchlist changes found.");

        markdown.append("## Portfolio Action Recommendations\n\n");
        if (materialActions.isEmpty()) {
            markdown.append("- No non-hold action recommendations. Review current holdings but do not force trades.\n\n");
        } else {
            for (PortfolioActionDto action : materialActions) {
                markdown.append("- **").append(action.getAction()).append(" ")
                        .append(action.getTicker()).append("**: ")
                        .append(action.getReason()).append(" Score: ")
                        .append(valueOrNa(action.getStockScore())).append(", source quality: ")
                        .append(valueOrNa(action.getSourceQualityScore())).append(".\n");
            }
            markdown.append("\n");
        }

        markdown.append("## Upcoming Events Next Week\n\n");
        if (upcoming.isEmpty()) {
            markdown.append("- No upcoming workflow items detected.\n\n");
        } else {
            for (UpcomingEventDto event : upcoming) {
                markdown.append("- **").append(event.getDueDate()).append(" ")
                        .append(nonBlank(event.getTicker(), "Portfolio")).append("**: ")
                        .append(event.getTitle()).append(" - ")
                        .append(event.getReason()).append("\n");
            }
            markdown.append("\n");
        }

        markdown.append("## What Is Probably Hype\n\n");
        List<SignalRow> hypeWarnings = signals.stream()
                .filter(signal -> nullToZero(signal.sourceQualityScore()) < 60
                        || "RSS_FEED".equalsIgnoreCase(signal.sourceType()))
                .limit(8)
                .toList();
        appendSignals(markdown, hypeWarnings, "No obvious hype warnings from recent stored signals.");

        markdown.append("## Manual Research Checklist\n\n");
        if (manualChecks.isEmpty()) {
            markdown.append("- No low-confidence or manual-review signals detected.\n\n");
        } else {
            for (SignalRow signal : manualChecks) {
                markdown.append("- Verify **").append(nonBlank(signal.ticker(), "Unknown")).append("**: ")
                        .append(signal.summary()).append(" Source quality: ")
                        .append(valueOrNa(signal.sourceQualityScore())).append(", confidence: ")
                        .append(valueOrNa(signal.confidenceScore())).append(".\n");
            }
            markdown.append("\n");
        }

        return markdown.toString();
    }

    private void appendSignals(StringBuilder markdown, List<SignalRow> signals, String emptyText) {
        if (signals.isEmpty()) {
            markdown.append("- ").append(emptyText).append("\n\n");
            return;
        }
        for (SignalRow signal : signals) {
            markdown.append("- **").append(nonBlank(signal.ticker(), "Unknown")).append("**: ")
                    .append(signal.summary()).append(" Score: ")
                    .append(valueOrNa(signal.stockScore())).append(", source: ")
                    .append(nonBlank(signal.sourceType(), "unknown")).append(".\n");
            if (signal.whatChanged() != null && !signal.whatChanged().isBlank()) {
                markdown.append("  What changed: ").append(signal.whatChanged()).append("\n");
            }
        }
        markdown.append("\n");
    }

    private List<SignalRow> recentSignals(int lookbackDays, int limit) {
        return jdbcTemplate.query("""
                select c.ticker, c.name as company_name, d.source_type, e.summary, e.what_changed,
                       e.bullish_score, e.bearish_score, e.source_quality_score, e.confidence_score,
                       e.requires_manual_review, e.created_at
                from extracted_events e
                left join companies c on c.id = e.company_id
                join raw_documents d on d.id = e.document_id
                where e.created_at >= now() - (? * interval '1 day')
                order by e.created_at desc
                limit ?
                """, (rs, rowNum) -> mapSignal(rs), lookbackDays, limit);
    }

    private SignalRow mapSignal(ResultSet rs) throws SQLException {
        Integer bullishScore = (Integer) rs.getObject("bullish_score");
        Integer bearishScore = (Integer) rs.getObject("bearish_score");
        Integer sourceQualityScore = (Integer) rs.getObject("source_quality_score");
        Integer stockScore = null;
        if (bullishScore != null && bearishScore != null && sourceQualityScore != null) {
            stockScore = Math.max(0, Math.min(100,
                    50 + ((bullishScore - bearishScore) * sourceQualityScore / 100) / 2));
        }

        return new SignalRow(
                rs.getString("ticker"),
                rs.getString("company_name"),
                rs.getString("source_type"),
                rs.getString("summary"),
                rs.getString("what_changed"),
                stockScore,
                sourceQualityScore,
                (Integer) rs.getObject("confidence_score"),
                rs.getBoolean("requires_manual_review"));
    }

    private ReportSummaryDto mapSummary(ResultSet rs, int rowNum) throws SQLException {
        return ReportSummaryDto.builder()
                .id(rs.getLong("id"))
                .reportType(rs.getString("report_type"))
                .title(rs.getString("title"))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .build();
    }

    private ReportDetailDto mapDetail(ResultSet rs, int rowNum) throws SQLException {
        return ReportDetailDto.builder()
                .id(rs.getLong("id"))
                .reportType(rs.getString("report_type"))
                .title(rs.getString("title"))
                .reportMarkdown(rs.getString("report_markdown"))
                .reportJson(rs.getString("report_json"))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .build();
    }

    private Long ensureDefaultUser() {
        List<Long> ids = jdbcTemplate.query(
                "select id from users where email = ? limit 1",
                (rs, rowNum) -> rs.getLong("id"),
                DEFAULT_USER_EMAIL);
        if (!ids.isEmpty()) {
            return ids.get(0);
        }

        return jdbcTemplate.queryForObject("""
                insert into users (email, display_name, password_hash, base_currency)
                values (?, 'Local User', 'local-dev-no-login', 'CAD')
                returning id
                """, Long.class, DEFAULT_USER_EMAIL);
    }

    private String reportJson(PortfolioActionReportResponse actionReport) {
        return String.format(Locale.US,
                "{\"totalMarketValueCad\":%s,\"actionCount\":%d}",
                actionReport.getTotalMarketValueCad() == null ? BigDecimal.ZERO : actionReport.getTotalMarketValueCad(),
                actionReport.getActions().size());
    }

    private String normalizeReportType(String reportType) {
        if (reportType == null || reportType.isBlank()) {
            return "weekly";
        }
        String normalized = reportType.trim().toLowerCase(Locale.US);
        if (!"daily".equals(normalized) && !"weekly".equals(normalized) && !"action".equals(normalized)) {
            throw new IllegalArgumentException("Unsupported report type: " + reportType);
        }
        return normalized;
    }

    private String reportJobName(String reportType) {
        return switch (reportType) {
            case "daily" -> "DailyDigestJob";
            case "action" -> "ActionReportJob";
            default -> "WeeklyReportJob";
        };
    }

    private String reportTitle(String reportType) {
        return switch (reportType) {
            case "daily" -> "Daily Signal Digest - " + LocalDate.now();
            case "action" -> "Portfolio Action Report - " + LocalDate.now();
            default -> "Weekly Market Intelligence Report - " + LocalDate.now();
        };
    }

    private Integer nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String valueOrNa(Object value) {
        return value == null ? "n/a" : value.toString();
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record SignalRow(
            String ticker,
            String companyName,
            String sourceType,
            String summary,
            String whatChanged,
            Integer stockScore,
            Integer sourceQualityScore,
            Integer confidenceScore,
            boolean requiresManualReview) {
    }
}
