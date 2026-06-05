package com.scout.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ingestion.scheduler.enabled", havingValue = "true")
public class ScheduledIngestionService {

    private final SecEdgarIngestionService secEdgarIngestionService;
    private final RssIngestionService rssIngestionService;

    @Value("${ingestion.sec.limit-per-company:3}")
    private int secLimitPerCompany;

    @Value("${ingestion.rss.limit-per-feed:5}")
    private int rssLimitPerFeed;

    @Value("${ingestion.rss.feeds:}")
    private String rssFeeds;

    @Scheduled(
            initialDelayString = "${ingestion.sec.initial-delay-ms:60000}",
            fixedDelayString = "${ingestion.sec.fixed-delay-ms:3600000}")
    public void runSecIngestion() {
        try {
            log.info("Running scheduled SEC EDGAR ingestion");
            secEdgarIngestionService.ingestRecentFilings(null, secLimitPerCompany);
        } catch (RuntimeException e) {
            log.error("Scheduled SEC EDGAR ingestion failed", e);
        }
    }

    @Scheduled(
            initialDelayString = "${ingestion.rss.initial-delay-ms:90000}",
            fixedDelayString = "${ingestion.rss.fixed-delay-ms:3600000}")
    public void runRssIngestion() {
        List<RssFeedSpec> feedSpecs = parseFeedSpecs(rssFeeds);
        if (feedSpecs.isEmpty()) {
            return;
        }

        for (RssFeedSpec feedSpec : feedSpecs) {
            try {
                log.info("Running scheduled RSS ingestion: feedUrl={}, ticker={}",
                        feedSpec.feedUrl(), feedSpec.ticker());
                rssIngestionService.ingestFeed(feedSpec.feedUrl(), feedSpec.ticker(), rssLimitPerFeed);
            } catch (RuntimeException e) {
                log.error("Scheduled RSS ingestion failed for {}", feedSpec.feedUrl(), e);
            }
        }
    }

    static List<RssFeedSpec> parseFeedSpecs(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(entry -> !entry.isBlank())
                .map(entry -> {
                    String[] parts = entry.split("\\|", 2);
                    String feedUrl = parts[0].trim();
                    String ticker = parts.length > 1 ? parts[1].trim().toUpperCase() : "";
                    return new RssFeedSpec(feedUrl, ticker.isBlank() ? null : ticker);
                })
                .toList();
    }

    record RssFeedSpec(String feedUrl, String ticker) {
    }
}
