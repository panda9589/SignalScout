package com.scout.application.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChunkDocumentResponse {
    private Long documentId;
    private Long jobRunId;
    private int chunksCreated;
}
