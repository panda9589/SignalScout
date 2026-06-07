package com.scout.application.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RssIngestionResponse {
    private Long jobRunId;
    private int itemsFound;
    private int documentsStored;
    private int duplicatesSkipped;
    private int autoMatchedDocuments;
    private int chunksCreated;
}
