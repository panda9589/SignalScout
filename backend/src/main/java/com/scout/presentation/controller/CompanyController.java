package com.scout.presentation.controller;

import com.scout.application.dto.CompanyDetailResponse;
import com.scout.application.dto.WatchlistSummaryDto;
import com.scout.application.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API endpoints for companies and watchlist
 */
@Slf4j
@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
public class CompanyController {
    
    private final DocumentService documentService;
    
    /**
     * GET /api/companies/{ticker}
     * Return company detail with latest score and recent events
     */
    @GetMapping("/{ticker}")
    public ResponseEntity<CompanyDetailResponse> getCompanyDetail(
            @PathVariable String ticker) {
        
        log.info("Fetching company detail: ticker={}", ticker);
        
        CompanyDetailResponse response = documentService.getCompanyDetail(ticker.toUpperCase());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /api/companies/watchlist/summary
     * Return all companies with latest scores for watchlist view
     */
    @GetMapping("/watchlist/summary")
    public ResponseEntity<List<WatchlistSummaryDto>> getWatchlistSummary() {
        
        log.info("Fetching watchlist summary");
        
        List<WatchlistSummaryDto> companies = documentService.getAllCompaniesSummary();
        
        return ResponseEntity.ok(companies);
    }
}
