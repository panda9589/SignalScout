package com.scout.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Extracted investment event from an AI analysis.
 * Contains bull/bear cases, risks, watch items, and confidence scores.
 */
@Entity
@Table(name = "extracted_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private RawDocument document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "event_date")
    private java.time.LocalDate eventDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "what_changed", columnDefinition = "TEXT")
    private String whatChanged;

    @Column(name = "bull_case", columnDefinition = "JSONB")
    private String bullCase;

    @Column(name = "bear_case", columnDefinition = "JSONB")
    private String bearCase;

    @Column(columnDefinition = "JSONB")
    private String risks;

    @Column(name = "watch_items", columnDefinition = "JSONB")
    private String watchItems;

    @Column(name = "bullish_score")
    private Integer bullishScore;

    @Column(name = "bearish_score")
    private Integer bearishScore;

    @Column(name = "source_quality_score")
    private Integer sourceQualityScore;

    @Column(name = "confidence_score")
    private Integer confidenceScore;

    @Column(name = "requires_manual_review", nullable = false)
    private Boolean requiresManualReview;

    @Column(name = "manual_review_reason", columnDefinition = "TEXT")
    private String manualReviewReason;

    @Column(name = "extraction_model", length = 100)
    private String extractionModel;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (requiresManualReview == null) {
            requiresManualReview = false;
        }
    }
}
