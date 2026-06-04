package com.scout.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scout.application.dto.ExtractionOutputDto;
import com.scout.domain.entity.ExtractedEvent;
import com.scout.domain.entity.RawDocument;
import com.scout.infrastructure.exception.ExtractionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service to call OpenAI Structured Outputs API for document extraction
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIExtractionService {
    
    @Value("${openai.api.key:}")
    private String openAiApiKey;
    
    @Value("${openai.api.endpoint:https://api.openai.com/v1/chat/completions}")
    private String openAiEndpoint;
    
    private final ObjectMapper objectMapper;
    
    private static final String MODEL = "gpt-4o-2024-08-06";
    private static final String EXTRACTION_SYSTEM_PROMPT = """
            You are an investment research analyst. Extract investment events from documents with structured JSON output.
            Focus on identifying catalysts, guidance changes, earnings, partnerships, management commentary, and other material events.
            Always provide your response in valid JSON format matching the specified schema.
            Be precise and cite evidence from the document.
            If the document doesn't contain clear investment relevance, still provide extraction with low relevance score and confidence.
            """;
    
    /**
     * Extract investment event from raw document text
     */
    public ExtractionOutputDto extractFromDocument(RawDocument document) {
        if (openAiApiKey.isEmpty()) {
            log.warn("OpenAI API key not configured, using mock extraction");
            return createMockExtraction(document);
        }
        
        String userPrompt = buildExtractionPrompt(document);
        
        try {
            String jsonResponse = callOpenAIStructuredOutput(userPrompt);
            ExtractionOutputDto extraction = parseExtractionResponse(jsonResponse);
            
            log.info("Successfully extracted event from document {}: ticker={}, eventType={}, confidence={}",
                    document.getId(), extraction.getTicker(), extraction.getEventType(), extraction.getConfidenceScore());
            
            return extraction;
            
        } catch (Exception e) {
            log.error("Failed to extract event from document {}", document.getId(), e);
            throw new ExtractionException("Failed to extract event from document: " + e.getMessage(), e);
        }
    }
    
    /**
     * Call OpenAI API with structured output format
     */
    private String callOpenAIStructuredOutput(String userPrompt) throws IOException {
        // For Phase 1 MVP, we'll use a simplified HTTP call
        // In production, use com.knuddels.jtokkit or similar for token counting
        
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
        
        String jsonPayload = """
                {
                  "model": "%s",
                  "messages": [
                    {
                      "role": "system",
                      "content": "%s"
                    },
                    {
                      "role": "user",
                      "content": "%s"
                    }
                  ],
                  "response_format": {
                    "type": "json_schema",
                    "json_schema": {
                      "name": "investment_event_extraction",
                      "schema": %s
                    }
                  },
                  "temperature": 0.7,
                  "max_tokens": 2000
                }
                """.formatted(
                MODEL,
                escapeJson(EXTRACTION_SYSTEM_PROMPT),
                escapeJson(userPrompt),
                getExtractionJsonSchema()
        );
        
        okhttp3.RequestBody body = okhttp3.RequestBody.create(
                jsonPayload,
                okhttp3.MediaType.parse("application/json")
        );
        
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(openAiEndpoint)
                .header("Authorization", "Bearer " + openAiApiKey)
                .header("Content-Type", "application/json")
                .post(body)
                .build();
        
        try (okhttp3.Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                throw new ExtractionException("OpenAI API error: " + response.code() + " - " + errorBody);
            }
            
            String responseBody = response.body().string();
            Map<String, Object> responseJson = objectMapper.readValue(responseBody, new TypeReference<>() {});
            
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseJson.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new ExtractionException("No choices in OpenAI response");
            }
            
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");
            
            // Log API usage for cost tracking
            Map<String, Object> usage = (Map<String, Object>) responseJson.get("usage");
            if (usage != null) {
                Integer promptTokens = (Integer) usage.get("prompt_tokens");
                Integer completionTokens = (Integer) usage.get("completion_tokens");
                log.info("OpenAI API usage: prompt={} tokens, completion={} tokens (est. cost ~${})", 
                        promptTokens, completionTokens, estimateCost(promptTokens, completionTokens));
            }
            
            return content;
        }
    }
    
    private ExtractionOutputDto parseExtractionResponse(String jsonResponse) throws IOException {
        return objectMapper.readValue(jsonResponse, ExtractionOutputDto.class);
    }
    
    private String buildExtractionPrompt(RawDocument document) {
        return """
                Please analyze the following document and extract investment event information.
                
                Document Title: %s
                Document Source Type: %s
                
                Document Content:
                ---
                %s
                ---
                
                Extract the most important investment event from this document. Be concise but complete.
                """.formatted(
                document.getTitle() != null ? document.getTitle() : "Untitled",
                document.getSourceType(),
                document.getRawText().substring(0, Math.min(3000, document.getRawText().length()))
        );
    }
    
    private String getExtractionJsonSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "schema_version": {
                      "type": "string",
                      "description": "Version of extraction schema"
                    },
                    "ticker": {
                      "type": "string",
                      "description": "Company ticker or UNKNOWN"
                    },
                    "company_name": {
                      "type": "string",
                      "description": "Company name"
                    },
                    "document_relevance": {
                      "type": "string",
                      "enum": ["high", "medium", "low"]
                    },
                    "event_type": {
                      "type": "string",
                      "enum": ["earnings_release", "guidance_raise", "guidance_cut", "product_launch", 
                               "partnership", "customer_win", "customer_loss", "management_commentary",
                               "analyst_upgrade", "analyst_downgrade", "estimate_revision", "insider_buying",
                               "insider_selling", "regulatory_risk", "macro_theme", "supply_constraint"]
                    },
                    "event_date": {
                      "type": "string",
                      "format": "date",
                      "description": "YYYY-MM-DD"
                    },
                    "summary": {
                      "type": "string",
                      "description": "1-2 sentence summary"
                    },
                    "what_changed": {
                      "type": "string",
                      "description": "What is materially new"
                    },
                    "evidence": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "properties": {
                          "text": {"type": "string"},
                          "location": {"type": "string"},
                          "evidence_type": {"type": "string"}
                        }
                      }
                    },
                    "bull_case": {
                      "type": "array",
                      "items": {"type": "string"}
                    },
                    "bear_case": {
                      "type": "array",
                      "items": {"type": "string"}
                    },
                    "risks": {
                      "type": "array",
                      "items": {"type": "string"}
                    },
                    "watch_items": {
                      "type": "array",
                      "items": {"type": "string"}
                    },
                    "source_quality_score": {
                      "type": "integer",
                      "minimum": 0,
                      "maximum": 100
                    },
                    "bullish_score": {
                      "type": "integer",
                      "minimum": 0,
                      "maximum": 100
                    },
                    "bearish_score": {
                      "type": "integer",
                      "minimum": 0,
                      "maximum": 100
                    },
                    "confidence_score": {
                      "type": "integer",
                      "minimum": 0,
                      "maximum": 100
                    },
                    "requires_manual_review": {
                      "type": "boolean"
                    },
                    "manual_review_reason": {
                      "type": "string"
                    }
                  },
                  "required": ["schema_version", "ticker", "event_type", "summary", "bullish_score", "bearish_score", "source_quality_score", "confidence_score"]
                }
                """;
    }
    
    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    private double estimateCost(int promptTokens, int completionTokens) {
        // GPT-4o pricing: $2.50 per 1M prompt tokens, $10 per 1M completion tokens
        double promptCost = (promptTokens / 1_000_000.0) * 2.50;
        double completionCost = (completionTokens / 1_000_000.0) * 10.0;
        return Math.round((promptCost + completionCost) * 10000) / 10000.0;
    }
    
    /**
     * Create a mock extraction for development/testing when API key not configured
     */
    private ExtractionOutputDto createMockExtraction(RawDocument document) {
        return ExtractionOutputDto.builder()
                .schemaVersion("1.0")
                .ticker("UNKNOWN")
                .companyName("Unknown Company")
                .documentRelevance("medium")
                .eventType("management_commentary")
                .eventDate(LocalDate.now())
                .summary("Mock extraction - OpenAI API not configured. Configure OPENAI_API_KEY environment variable.")
                .whatChanged("Development mode")
                .bullishScore(50)
                .bearishScore(40)
                .sourceQualityScore(60)
                .confidenceScore(30)
                .bullCase(List.of("Mock bullish point"))
                .bearCase(List.of("Mock bearish point"))
                .risks(List.of("Not a real extraction"))
                .watchItems(List.of("Configure OpenAI API"))
                .requiresManualReview(true)
                .manualReviewReason("This is a mock extraction - configure OpenAI API for real extractions")
                .build();
    }
}
