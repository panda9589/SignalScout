package com.scout.presentation.controller;

import com.scout.domain.entity.Company;
import com.scout.domain.entity.DataSource;
import com.scout.domain.entity.Theme;
import com.scout.domain.repository.CompanyRepository;
import com.scout.domain.repository.DataSourceRepository;
import com.scout.domain.repository.ThemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin endpoints for seeding initial data
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final CompanyRepository companyRepository;
    private final ThemeRepository themeRepository;
    private final DataSourceRepository dataSourceRepository;
    
    /**
     * POST /api/admin/seed-companies
     * Populate 5 real companies: MRVL, AVGO, NVDA, MSFT, AMD
     */
    @PostMapping("/seed-companies")
    public ResponseEntity<String> seedCompanies() {
        
        List<Company> companies = List.of(
                Company.builder()
                        .ticker("MRVL")
                        .name("Marvell Technology")
                        .cik("1141021")
                        .exchange("NASDAQ")
                        .country("US")
                        .sector("Semiconductors")
                        .industry("Semiconductor Equipment & Materials")
                        .currency("USD")
                        .build(),
                Company.builder()
                        .ticker("AVGO")
                        .name("Broadcom Inc.")
                        .cik("1397687")
                        .exchange("NASDAQ")
                        .country("US")
                        .sector("Semiconductors")
                        .industry("Semiconductor Equipment & Materials")
                        .currency("USD")
                        .build(),
                Company.builder()
                        .ticker("NVDA")
                        .name("NVIDIA Corporation")
                        .cik("1045810")
                        .exchange("NASDAQ")
                        .country("US")
                        .sector("Semiconductors")
                        .industry("Semiconductor Equipment & Materials")
                        .currency("USD")
                        .build(),
                Company.builder()
                        .ticker("MSFT")
                        .name("Microsoft Corporation")
                        .cik("0000789019")
                        .exchange("NASDAQ")
                        .country("US")
                        .sector("Technology")
                        .industry("Software")
                        .currency("USD")
                        .build(),
                Company.builder()
                        .ticker("AMD")
                        .name("Advanced Micro Devices Inc.")
                        .cik("0001045098")
                        .exchange("NASDAQ")
                        .country("US")
                        .sector("Semiconductors")
                        .industry("Semiconductor Equipment & Materials")
                        .currency("USD")
                        .build()
        );
        
        companies.forEach(company -> {
            if (companyRepository.findByTicker(company.getTicker()).isEmpty()) {
                companyRepository.save(company);
                log.info("Seeded company: {}", company.getTicker());
            }
        });
        
        return ResponseEntity.ok("Companies seeded successfully");
    }
    
    /**
     * POST /api/admin/seed-themes
     * Populate themes: AI/ML, Semiconductors, Enterprise Software
     */
    @PostMapping("/seed-themes")
    public ResponseEntity<String> seedThemes() {
        
        List<Theme> themes = List.of(
                Theme.builder()
                        .name("AI/ML")
                        .description("Artificial Intelligence and Machine Learning companies and trends")
                        .createdAt(LocalDateTime.now())
                        .build(),
                Theme.builder()
                        .name("Semiconductors")
                        .description("Semiconductor design, manufacturing, and equipment companies")
                        .createdAt(LocalDateTime.now())
                        .build(),
                Theme.builder()
                        .name("Enterprise Software")
                        .description("Enterprise software and cloud infrastructure providers")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        
        themes.forEach(theme -> {
            if (themeRepository.findByName(theme.getName()).isEmpty()) {
                themeRepository.save(theme);
                log.info("Seeded theme: {}", theme.getName());
            }
        });
        
        return ResponseEntity.ok("Themes seeded successfully");
    }
    
    /**
     * POST /api/admin/seed-sources
     * Populate sources: SEC, Manual
     */
    @PostMapping("/seed-sources")
    public ResponseEntity<String> seedSources() {
        
        List<DataSource> sources = List.of(
                DataSource.builder()
                        .sourceType("SEC")
                        .name("SEC EDGAR")
                        .baseUrl("https://www.sec.gov/cgi-bin/")
                        .trustLevel("high")
                        .enabled(true)
                        .pollIntervalMinutes(1440)
                        .createdAt(LocalDateTime.now())
                        .build(),
                DataSource.builder()
                        .sourceType("Manual")
                        .name("Manual Paste")
                        .trustLevel("medium")
                        .enabled(true)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        
        sources.forEach(source -> {
            if (dataSourceRepository.findBySourceType(source.getSourceType()).isEmpty()) {
                dataSourceRepository.save(source);
                log.info("Seeded source: {}", source.getSourceType());
            }
        });
        
        return ResponseEntity.ok("Sources seeded successfully");
    }
}
