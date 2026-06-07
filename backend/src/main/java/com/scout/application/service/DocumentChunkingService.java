package com.scout.application.service;

import com.scout.application.dto.ChunkDocumentResponse;
import com.scout.application.dto.DocumentChunkDto;
import com.scout.application.dto.DocumentSearchResultDto;
import com.scout.domain.entity.DocumentChunk;
import com.scout.domain.entity.JobRun;
import com.scout.domain.entity.RawDocument;
import com.scout.domain.repository.DocumentChunkRepository;
import com.scout.domain.repository.RawDocumentRepository;
import com.scout.infrastructure.exception.ResourceNotFoundException;
import com.scout.infrastructure.util.TextChunker;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentChunkingService {

    private static final int MAX_CHUNK_WORDS = 450;
    private static final int OVERLAP_WORDS = 60;

    private final RawDocumentRepository rawDocumentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final JobRunService jobRunService;

    @Transactional
    public ChunkDocumentResponse chunkDocument(Long documentId) {
        RawDocument document = rawDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));

        JobRun jobRun = jobRunService.start("DocumentChunkingJob", 1);
        try {
            int chunksCreated = recreateChunks(document);
            JobRun completed = jobRunService.complete(jobRun, 1);
            return ChunkDocumentResponse.builder()
                    .documentId(document.getId())
                    .jobRunId(completed.getId())
                    .chunksCreated(chunksCreated)
                    .build();
        } catch (RuntimeException e) {
            jobRunService.fail(jobRun, e);
            throw e;
        }
    }

    @Transactional
    public int recreateChunks(RawDocument document) {
        documentChunkRepository.deleteByDocumentId(document.getId());
        documentChunkRepository.flush();

        List<String> chunkTexts = TextChunker.chunkByWords(document.getRawText(), MAX_CHUNK_WORDS, OVERLAP_WORDS);
        for (int index = 0; index < chunkTexts.size(); index++) {
            String chunkText = chunkTexts.get(index);
            DocumentChunk chunk = DocumentChunk.builder()
                    .document(document)
                    .chunkIndex(index)
                    .chunkText(chunkText)
                    .tokenCount(TextChunker.approximateTokenCount(chunkText))
                    .build();
            documentChunkRepository.save(chunk);
        }

        return chunkTexts.size();
    }

    @Transactional(readOnly = true)
    public List<DocumentChunkDto> getDocumentChunks(Long documentId) {
        if (!rawDocumentRepository.existsById(documentId)) {
            throw new ResourceNotFoundException("Document not found: " + documentId);
        }

        return documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentSearchResultDto> searchChunks(String query, int limit) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("q query parameter is required");
        }

        int safeLimit = Math.max(1, Math.min(limit, 50));
        return documentChunkRepository.searchChunkText(query.trim(), PageRequest.of(0, safeLimit))
                .stream()
                .map(chunk -> toSearchResult(chunk, query.trim()))
                .toList();
    }

    private DocumentChunkDto toDto(DocumentChunk chunk) {
        return DocumentChunkDto.builder()
                .id(chunk.getId())
                .documentId(chunk.getDocument().getId())
                .chunkIndex(chunk.getChunkIndex())
                .chunkText(chunk.getChunkText())
                .tokenCount(chunk.getTokenCount())
                .createdAt(chunk.getCreatedAt())
                .build();
    }

    private DocumentSearchResultDto toSearchResult(DocumentChunk chunk, String query) {
        RawDocument document = chunk.getDocument();
        String ticker = document.getCompany() != null ? document.getCompany().getTicker() : null;
        return DocumentSearchResultDto.builder()
                .documentId(document.getId())
                .chunkId(chunk.getId())
                .chunkIndex(chunk.getChunkIndex())
                .ticker(ticker)
                .title(document.getTitle())
                .sourceType(document.getSourceType())
                .snippet(buildSnippet(chunk.getChunkText(), query))
                .build();
    }

    private String buildSnippet(String text, String query) {
        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();
        int matchIndex = lowerText.indexOf(lowerQuery);
        if (matchIndex < 0) {
            return text.length() <= 240 ? text : text.substring(0, 240);
        }

        int start = Math.max(0, matchIndex - 80);
        int end = Math.min(text.length(), matchIndex + query.length() + 160);
        String prefix = start > 0 ? "..." : "";
        String suffix = end < text.length() ? "..." : "";
        return prefix + text.substring(start, end) + suffix;
    }
}
