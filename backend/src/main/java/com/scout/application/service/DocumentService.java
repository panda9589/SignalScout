package com.scout.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scout.application.dto.*;
import com.scout.domain.entity.Company;
import com.scout.domain.entity.DataSource;
import com.scout.domain.entity.ExtractedEvent;
import com.scout.domain.entity.RawDocument;
import com.scout.domain.repository.CompanyRepository;
import com.scout.domain.repository.DataSourceRepository;
import com.scout.domain.repository.ExtractedEventRepository;
import com.scout.domain.repository.RawDocumentRepository;
import com.scout.infrastructure.exception.ResourceNotFoundException;
import com.scout.infrastructure.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing documents and extraction workflow
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {
    
    private final RawDocumentRepository rawDocumentRepository;
    private final CompanyRepository companyRepository;
    private final DataSourceRepository dataSourceRepository;
    private final ExtractedEventRepository extractedEventRepository;
    private final OpenAIExtractionService extractionService;
    private final ScoringService scoringService;
    private final ObjectMapper objectMapper;
    
    /**
     * Process manually pasted document: store, extract, score
     */
    @Transactional
    public DocumentExtractedResponse processManualDocumentPaste(ManualDocumentPasteRequest request) {
        
        // Validate inputs
        Long companyId;
        try {
            companyId = Long.parseLong(request.getCompanyId());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("company_id must be a numeric company id");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));
        
        DataSource source = dataSourceRepository.findBySourceType("MANUAL_PASTE")
                .or(() -> dataSourceRepository.findBySourceType("Manual"))
                .orElseThrow(() -> new ResourceNotFoundException("Manual source not configured"));
        
        // Check for duplicate by content hash
        String contentHash = HashUtil.sha256(request.getRawText());
        if (rawDocumentRepository.findByContentHash(contentHash).isPresent()) {
            log.warn("Duplicate document detected for company {}", companyId);
            throw new RuntimeException("This document has already been processed (duplicate content hash)");
        }
        
        // Create and store RawDocument
        RawDocument document = RawDocument.builder()
                .company(company)
                .source(source)
                .sourceType(source.getSourceType())
                .title("Manual Paste - " + LocalDateTime.now())
                .rawText(request.getRawText())
                .contentHash(contentHash)
                .processingStatus("completed")
                .retrievedAt(LocalDateTime.now())
                .build();
        
        document = rawDocumentRepository.save(document);
        log.info("Stored raw document {}: company={}", document.getId(), company.getTicker());
        
        // Call OpenAI for extraction
        ExtractionOutputDto extraction = extractionService.extractFromDocument(document);
        
        // Create ExtractedEvent
        ExtractedEvent event = ExtractedEvent.builder()
                .document(document)
                .company(company)
                .eventType(extraction.getEventType())
                .eventDate(extraction.getEventDate())
                .summary(extraction.getSummary())
                .whatChanged(extraction.getWhatChanged())
                .bullCase(toJson(extraction.getBullCase()))
                .bearCase(toJson(extraction.getBearCase()))
                .risks(toJson(extraction.getRisks()))
                .watchItems(toJson(extraction.getWatchItems()))
                .bullishScore(extraction.getBullishScore())
                .bearishScore(extraction.getBearishScore())
                .sourceQualityScore(extraction.getSourceQualityScore())
                .confidenceScore(extraction.getConfidenceScore())
                .requiresManualReview(extraction.getRequiresManualReview() != null ? extraction.getRequiresManualReview() : false)
                .manualReviewReason(extraction.getManualReviewReason())
                .extractionModel("gpt-4o")
                .build();
        
        event = extractedEventRepository.save(event);
        log.info("Created extracted event {}: eventType={}, bullish={}, bearish={}",
                event.getId(), event.getEventType(), event.getBullishScore(), event.getBearishScore());
        
        // Compute stock score
        Integer stockScore = scoringService.computeStockScore(event);
        String recommendation = scoringService.generateRecommendation(stockScore);
        
        // Build response
        return DocumentExtractedResponse.builder()
                .documentId(document.getId())
                .eventId(event.getId())
                .ticker(company.getTicker())
                .companyName(company.getName())
                .eventType(event.getEventType())
                .summary(event.getSummary())
                .whatChanged(event.getWhatChanged())
                .bullishScore(event.getBullishScore())
                .bearishScore(event.getBearishScore())
                .sourceQualityScore(event.getSourceQualityScore())
                .confidenceScore(event.getConfidenceScore())
                .stockScore(stockScore)
                .recommendation(recommendation)
                .bullCase(extraction.getBullCase())
                .bearCase(extraction.getBearCase())
                .risks(extraction.getRisks())
                .watchItems(extraction.getWatchItems())
                .requiresManualReview(event.getRequiresManualReview())
                .manualReviewReason(event.getManualReviewReason())
                .createdAt(event.getCreatedAt())
                .build();
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize extracted JSON field", e);
        }
    }
    
    /**
     * Get company detail with latest score and recent events
     */
    @Transactional(readOnly = true)
    public CompanyDetailResponse getCompanyDetail(String ticker) {
        Company company = companyRepository.findByTicker(ticker)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + ticker));
        
        List<ExtractedEvent> recentEvents = extractedEventRepository.findByCompanyId(
                company.getId(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        
        Integer latestScore = null;
        String latestRecommendation = null;
        
        if (!recentEvents.isEmpty()) {
            ExtractedEvent latestEvent = recentEvents.get(0);
            latestScore = scoringService.computeStockScore(latestEvent);
            latestRecommendation = scoringService.generateRecommendation(latestScore);
        }
        
        CompanyDto companyDto = CompanyDto.builder()
                .id(company.getId())
                .ticker(company.getTicker())
                .name(company.getName())
                .sector(company.getSector())
                .industry(company.getIndustry())
                .cik(company.getCik())
                .exchange(company.getExchange())
                .country(company.getCountry())
                .currency(company.getCurrency())
                .build();
        
        List<ExtractedEventDto> eventDtos = recentEvents.stream()
                .map(this::mapEventToDto)
                .collect(Collectors.toList());
        
        return CompanyDetailResponse.builder()
                .company(companyDto)
                .latestStockScore(latestScore)
                .latestRecommendation(latestRecommendation)
                .recentEvents(eventDtos)
                .build();
    }
    
    /**
     * Get all companies with latest scores for watchlist view
     */
    @Transactional(readOnly = true)
    public List<WatchlistSummaryDto> getAllCompaniesSummary() {
        return companyRepository.findAll().stream()
                .map(company -> {
                    List<ExtractedEvent> recentEvents = extractedEventRepository.findByCompanyId(
                            company.getId(),
                            Sort.by(Sort.Direction.DESC, "createdAt")
                    );
                    
                    Integer latestScore = null;
                    String latestRecommendation = null;
                    
                    if (!recentEvents.isEmpty()) {
                        ExtractedEvent latestEvent = recentEvents.get(0);
                        latestScore = scoringService.computeStockScore(latestEvent);
                        latestRecommendation = scoringService.generateRecommendation(latestScore);
                    }
                    
                    return WatchlistSummaryDto.builder()
                            .companyId(company.getId())
                            .ticker(company.getTicker())
                            .name(company.getName())
                            .sector(company.getSector())
                            .latestStockScore(latestScore)
                            .latestRecommendation(latestRecommendation)
                            .recentEventCount((long) recentEvents.size())
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    private ExtractedEventDto mapEventToDto(ExtractedEvent event) {
        List<String> bullCase = null;
        List<String> bearCase = null;
        List<String> risks = null;
        List<String> watchItems = null;
        
        try {
            if (event.getBullCase() != null) {
                bullCase = objectMapper.readValue(event.getBullCase(), List.class);
            }
            if (event.getBearCase() != null) {
                bearCase = objectMapper.readValue(event.getBearCase(), List.class);
            }
            if (event.getRisks() != null) {
                risks = objectMapper.readValue(event.getRisks(), List.class);
            }
            if (event.getWatchItems() != null) {
                watchItems = objectMapper.readValue(event.getWatchItems(), List.class);
            }
        } catch (Exception e) {
            log.warn("Failed to deserialize JSON fields for event {}", event.getId());
        }
        
        return ExtractedEventDto.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .eventDate(event.getEventDate())
                .summary(event.getSummary())
                .whatChanged(event.getWhatChanged())
                .bullishScore(event.getBullishScore())
                .bearishScore(event.getBearishScore())
                .sourceQualityScore(event.getSourceQualityScore())
                .confidenceScore(event.getConfidenceScore())
                .bullCase(bullCase)
                .bearCase(bearCase)
                .risks(risks)
                .watchItems(watchItems)
                .requiresManualReview(event.getRequiresManualReview())
                .manualReviewReason(event.getManualReviewReason())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
