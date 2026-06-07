package com.scout.presentation.controller;

import com.scout.application.dto.PortfolioActionReportResponse;
import com.scout.application.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final PortfolioService portfolioService;

    @PostMapping("/generate")
    public ResponseEntity<PortfolioActionReportResponse> generateReport() {
        return ResponseEntity.ok(portfolioService.generateActionReport());
    }
}
