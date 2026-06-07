package com.scout.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_runs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", nullable = false)
    private String jobName;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(nullable = false)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "documents_found")
    private Integer documentsFound;

    @Column(name = "documents_processed")
    private Integer documentsProcessed;

    @Column(name = "ai_calls")
    private Integer aiCalls;

    @Column(name = "estimated_cost_usd")
    private BigDecimal estimatedCostUsd;

    @Column(name = "metadata_json", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadataJson;

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "running";
        }
        if (documentsFound == null) {
            documentsFound = 0;
        }
        if (documentsProcessed == null) {
            documentsProcessed = 0;
        }
        if (aiCalls == null) {
            aiCalls = 0;
        }
    }
}
