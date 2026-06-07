package com.scout.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentChunkDto {
    private Long id;
    private Long documentId;
    private Integer chunkIndex;
    private String chunkText;
    private Integer tokenCount;
    private LocalDateTime createdAt;
}
