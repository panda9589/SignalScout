package com.scout.application.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmbeddingJobResponse {
    private Long jobRunId;
    private int chunksFound;
    private int embeddingsCreated;
    private int aiCalls;
    private String modelName;
}
