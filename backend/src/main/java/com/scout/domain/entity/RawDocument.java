package com.scout.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Raw document from any source (SEC filing, email, news, transcript).
 * Contains the full text and metadata. Processing happens on chunks of this document.
 */
@Entity
@Table(name = "raw_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private DataSource source;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "external_id", length = 255)
    private String externalId;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 255)
    private String author;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "retrieved_at", nullable = false)
    private LocalDateTime retrievedAt;

    @Column(name = "content_hash", nullable = false, unique = true, length = 64)
    private String contentHash;

    @Column(name = "raw_text", nullable = false, columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "metadata_json", columnDefinition = "JSONB")
    private String metadataJson;

    @Column(name = "processing_status", nullable = false, length = 50)
    private String processingStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        retrievedAt = LocalDateTime.now();
        if (processingStatus == null) {
            processingStatus = "new";
        }
    }
}
