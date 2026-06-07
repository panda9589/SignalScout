package com.scout.presentation.controller;

import com.scout.application.dto.EmbeddingJobResponse;
import com.scout.application.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/embeddings")
@RequiredArgsConstructor
public class EmbeddingController {

    private final EmbeddingService embeddingService;

    @PostMapping("/run")
    public ResponseEntity<EmbeddingJobResponse> generateMissingEmbeddings(
            @RequestParam(value = "limit", defaultValue = "100") int limit) {
        return ResponseEntity.ok(embeddingService.generateMissingEmbeddings(limit));
    }
}
