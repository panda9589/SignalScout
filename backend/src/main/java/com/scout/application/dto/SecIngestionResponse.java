package com.scout.application.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SecIngestionResponse {
    private Long jobRunId;
    private int companiesScanned;
    private int filingsFound;
    private int documentsStored;
    private int duplicatesSkipped;
    private int chunksCreated;
}
