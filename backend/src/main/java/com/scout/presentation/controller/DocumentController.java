package com.scout.presentation.controller;

import com.scout.application.dto.ManualDocumentPasteRequest;
import com.scout.application.dto.DocumentExtractedResponse;
import com.scout.application.dto.CompanyDetailResponse;
import com.scout.application.dto.WatchlistSummaryDto;
import com.scout.application.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API endpoints for document processing and extraction
 */
@Slf4j
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {
    
    private final DocumentService documentService;
    
    /**
     * POST /api/documents/manual
     * Accept raw_text, company_id, source_type
     * Store as RawDocument
     * Call OpenAI for extraction
     * Return extracted event + recommendation
     */
    @PostMapping("/manual")
    public ResponseEntity<DocumentExtractedResponse> pasteDocument(
            @Valid @RequestBody ManualDocumentPasteRequest request) {
        
        log.info("Processing manual document paste: company_id={}, source_type={}",
                request.getCompanyId(), request.getSourceType());
        
        DocumentExtractedResponse response = documentService.processManualDocumentPaste(request);
        
        log.info("Successfully processed document: event_id={}, recommendation={}, score={}",
                response.getEventId(), response.getRecommendation(), response.getStockScore());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
