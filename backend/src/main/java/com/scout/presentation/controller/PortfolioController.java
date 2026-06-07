package com.scout.presentation.controller;

import com.scout.application.dto.*;
import com.scout.application.service.PortfolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping("/accounts")
    public ResponseEntity<List<PortfolioAccountDto>> getAccounts() {
        return ResponseEntity.ok(portfolioService.getAccounts());
    }

    @PostMapping("/accounts")
    public ResponseEntity<PortfolioAccountDto> createAccount(
            @Valid @RequestBody CreatePortfolioAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portfolioService.createAccount(request));
    }

    @PutMapping("/accounts/{id}")
    public ResponseEntity<PortfolioAccountDto> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody CreatePortfolioAccountRequest request) {
        return ResponseEntity.ok(portfolioService.updateAccount(id, request));
    }

    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        portfolioService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/holdings")
    public ResponseEntity<List<PortfolioHoldingDto>> getHoldings() {
        return ResponseEntity.ok(portfolioService.getHoldings());
    }

    @PostMapping("/holdings")
    public ResponseEntity<PortfolioHoldingDto> createHolding(
            @Valid @RequestBody UpsertPortfolioHoldingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portfolioService.createHolding(request));
    }

    @PutMapping("/holdings/{id}")
    public ResponseEntity<PortfolioHoldingDto> updateHolding(
            @PathVariable Long id,
            @Valid @RequestBody UpsertPortfolioHoldingRequest request) {
        return ResponseEntity.ok(portfolioService.updateHolding(id, request));
    }

    @DeleteMapping("/holdings/{id}")
    public ResponseEntity<Void> deleteHolding(@PathVariable Long id) {
        portfolioService.deleteHolding(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/risk-settings")
    public ResponseEntity<RiskSettingsDto> getRiskSettings() {
        return ResponseEntity.ok(portfolioService.getRiskSettings());
    }

    @PutMapping("/risk-settings")
    public ResponseEntity<RiskSettingsDto> updateRiskSettings(
            @RequestBody RiskSettingsDto request) {
        return ResponseEntity.ok(portfolioService.updateRiskSettings(request));
    }
}
