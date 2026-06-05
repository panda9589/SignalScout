package com.scout.application.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ManualDocumentPasteRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesSnakeCaseApiFields() throws Exception {
        String json = """
                {
                  "raw_text": "NVIDIA AI demand improved.",
                  "company_id": "3",
                  "source_type": "MANUAL_PASTE"
                }
                """;

        ManualDocumentPasteRequest request = objectMapper.readValue(json, ManualDocumentPasteRequest.class);

        assertThat(request.getRawText()).isEqualTo("NVIDIA AI demand improved.");
        assertThat(request.getCompanyId()).isEqualTo("3");
        assertThat(request.getSourceType()).isEqualTo("MANUAL_PASTE");
    }

    @Test
    void stillDeserializesCamelCaseFields() throws Exception {
        String json = """
                {
                  "rawText": "NVIDIA AI demand improved.",
                  "companyId": "3",
                  "sourceType": "MANUAL_PASTE"
                }
                """;

        ManualDocumentPasteRequest request = objectMapper.readValue(json, ManualDocumentPasteRequest.class);

        assertThat(request.getRawText()).isEqualTo("NVIDIA AI demand improved.");
        assertThat(request.getCompanyId()).isEqualTo("3");
        assertThat(request.getSourceType()).isEqualTo("MANUAL_PASTE");
    }
}
