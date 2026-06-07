package com.scout.application.service;

import com.scout.domain.entity.ExtractedEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScoringServiceTest {

    private final ScoringService scoringService = new ScoringService();

    @Test
    void computeStockScoreReturnsNeutralWhenEventIsMissing() {
        assertThat(scoringService.computeStockScore(null)).isEqualTo(50);
    }

    @Test
    void computeStockScoreReturnsNeutralWhenBullishScoreIsMissing() {
        ExtractedEvent event = ExtractedEvent.builder()
                .bearishScore(20)
                .sourceQualityScore(90)
                .build();

        assertThat(scoringService.computeStockScore(event)).isEqualTo(50);
    }

    @Test
    void computeStockScoreWeightsBullBearSpreadBySourceQuality() {
        ExtractedEvent event = ExtractedEvent.builder()
                .bullishScore(90)
                .bearishScore(10)
                .sourceQualityScore(80)
                .build();

        assertThat(scoringService.computeStockScore(event)).isEqualTo(72);
    }

    @Test
    void computeStockScoreClampsLowerBound() {
        ExtractedEvent event = ExtractedEvent.builder()
                .bullishScore(0)
                .bearishScore(100)
                .sourceQualityScore(100)
                .build();

        assertThat(scoringService.computeStockScore(event)).isZero();
    }

    @Test
    void generateRecommendationUsesPhaseOneThresholds() {
        assertThat(scoringService.generateRecommendation(80)).isEqualTo("BUY");
        assertThat(scoringService.generateRecommendation(60)).isEqualTo("WATCH");
        assertThat(scoringService.generateRecommendation(40)).isEqualTo("HOLD");
        assertThat(scoringService.generateRecommendation(39)).isEqualTo("AVOID");
        assertThat(scoringService.generateRecommendation(null)).isEqualTo("HOLD");
    }
}
