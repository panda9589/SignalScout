package com.scout.presentation.controller;

import com.scout.application.dto.ChunkDocumentResponse;
import com.scout.application.dto.DocumentChunkDto;
import com.scout.application.dto.DocumentExtractedResponse;
import com.scout.application.dto.DocumentSearchResultDto;
import com.scout.application.dto.ManualDocumentPasteRequest;
import com.scout.application.service.DocumentChunkingService;
import com.scout.application.service.DocumentService;
import com.scout.application.service.EmbeddingService;
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
    private final DocumentChunkingService documentChunkingService;
    private final EmbeddingService embeddingService;
    
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

    @PostMapping("/{id}/chunk")
    public ResponseEntity<ChunkDocumentResponse> chunkDocument(@PathVariable Long id) {
        return ResponseEntity.ok(documentChunkingService.chunkDocument(id));
    }

    @GetMapping("/{id}/chunks")
    public ResponseEntity<List<DocumentChunkDto>> getChunks(@PathVariable Long id) {
        return ResponseEntity.ok(documentChunkingService.getDocumentChunks(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<DocumentSearchResultDto>> searchDocuments(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(documentChunkingService.searchChunks(query, limit));
    }

    @GetMapping("/semantic-search")
    public ResponseEntity<List<DocumentSearchResultDto>> semanticSearchDocuments(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(embeddingService.semanticSearch(query, limit));
    }
}
