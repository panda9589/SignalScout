package com.scout.presentation.controller;

import com.scout.application.dto.SecIngestionResponse;
import com.scout.application.dto.RssIngestionResponse;
import com.scout.application.service.RssIngestionService;
import com.scout.application.service.SecEdgarIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ingestion")
@RequiredArgsConstructor
public class IngestionController {

    private final SecEdgarIngestionService secEdgarIngestionService;
    private final RssIngestionService rssIngestionService;

    @PostMapping("/sec/run")
    public ResponseEntity<SecIngestionResponse> runSecIngestion(
            @RequestParam(value = "ticker", required = false) String ticker,
            @RequestParam(value = "limit", defaultValue = "3") int limit) {
        return ResponseEntity.ok(secEdgarIngestionService.ingestRecentFilings(ticker, limit));
    }

    @PostMapping("/sec/documents/{documentId}/reprocess")
    public ResponseEntity<SecIngestionResponse> reprocessSecDocument(@PathVariable Long documentId) {
        return ResponseEntity.ok(secEdgarIngestionService.reprocessStoredFiling(documentId));
    }

    @PostMapping("/rss/run")
    public ResponseEntity<RssIngestionResponse> runRssIngestion(
            @RequestParam("feedUrl") String feedUrl,
            @RequestParam(value = "ticker", required = false) String ticker,
            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        return ResponseEntity.ok(rssIngestionService.ingestFeed(feedUrl, ticker, limit));
    }
}
