package com.scout.application.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ExtractionOutputDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void deserializesStructuredOutputSnakeCaseFields() throws Exception {
        String json = """
                {
                  "schema_version": "1.0",
                  "ticker": "NVDA",
                  "company_name": "NVIDIA Corporation",
                  "document_relevance": "high",
                  "event_type": "guidance_raise",
                  "event_date": "2026-06-04",
                  "summary": "Management raised guidance.",
                  "what_changed": "Guidance moved higher.",
                  "evidence": [
                    {
                      "text": "Management raised guidance.",
                      "location": "paragraph 2",
                      "evidence_type": "management_commentary"
                    }
                  ],
                  "bull_case": ["Data center demand is accelerating."],
                  "bear_case": ["Valuation risk remains high."],
                  "risks": ["Customer concentration."],
                  "watch_items": ["Next quarter data center revenue."],
                  "source_quality_score": 95,
                  "bullish_score": 85,
                  "bearish_score": 20,
                  "confidence_score": 90,
                  "requires_manual_review": false,
                  "manual_review_reason": ""
                }
                """;

        ExtractionOutputDto dto = objectMapper.readValue(json, ExtractionOutputDto.class);

        assertThat(dto.getSchemaVersion()).isEqualTo("1.0");
        assertThat(dto.getCompanyName()).isEqualTo("NVIDIA Corporation");
        assertThat(dto.getDocumentRelevance()).isEqualTo("high");
        assertThat(dto.getEventType()).isEqualTo("guidance_raise");
        assertThat(dto.getEventDate()).isEqualTo(LocalDate.of(2026, 6, 4));
        assertThat(dto.getWhatChanged()).isEqualTo("Guidance moved higher.");
        assertThat(dto.getBullCase()).containsExactly("Data center demand is accelerating.");
        assertThat(dto.getEvidence()).hasSize(1);
        assertThat(dto.getEvidence().get(0).getEvidenceType()).isEqualTo("management_commentary");
        assertThat(dto.getSourceQualityScore()).isEqualTo(95);
        assertThat(dto.getRequiresManualReview()).isFalse();
    }
}
