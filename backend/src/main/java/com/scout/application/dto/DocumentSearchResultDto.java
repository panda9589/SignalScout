package com.scout.application.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentSearchResultDto {
    private Long documentId;
    private Long chunkId;
    private Integer chunkIndex;
    private String ticker;
    private String title;
    private String sourceType;
    private String snippet;
    private Double distance;
}
