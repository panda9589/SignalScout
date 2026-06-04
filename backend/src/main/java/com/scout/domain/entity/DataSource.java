package com.scout.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data source configuration (SEC EDGAR, Gmail, RSS, Manual, etc.)
 */
@Entity
@Table(name = "sources")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "trust_level", length = 20)
    private String trustLevel;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(name = "poll_interval_minutes")
    private Integer pollIntervalMinutes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (enabled == null) {
            enabled = true;
        }
    }
}
