package com.scout.presentation.controller;

import com.scout.application.dto.InvestmentThesisDto;
import com.scout.application.dto.UpsertInvestmentThesisRequest;
import com.scout.application.service.PortfolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/theses")
@RequiredArgsConstructor
public class ThesisController {

    private final PortfolioService portfolioService;

    @GetMapping
    public ResponseEntity<List<InvestmentThesisDto>> getTheses() {
        return ResponseEntity.ok(portfolioService.getTheses());
    }

    @PostMapping
    public ResponseEntity<InvestmentThesisDto> createThesis(
            @Valid @RequestBody UpsertInvestmentThesisRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portfolioService.createThesis(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InvestmentThesisDto> updateThesis(
            @PathVariable Long id,
            @Valid @RequestBody UpsertInvestmentThesisRequest request) {
        return ResponseEntity.ok(portfolioService.updateThesis(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteThesis(@PathVariable Long id) {
        portfolioService.deleteThesis(id);
        return ResponseEntity.noContent().build();
    }
}
