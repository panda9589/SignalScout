package com.scout.application.service;

import com.scout.application.dto.ExtractionOutputDto;
import com.scout.domain.entity.ExtractedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service to compute stock scores and generate recommendations
 * Based on the scoring formula from the design specification
 */
@Slf4j
@Service
public class ScoringService {
    
    /**
     * Compute stock score from extracted event
     * Formula from design: 
     * positive_score = 0.25*catalyst + 0.20*fundamental + 0.20*estimates + 0.15*valuation + 0.10*quality + 0.10*momentum
     * risk_penalty = max(0, risk - 60) * 0.35
     * thesis_penalty = (thesis < 40) ? 10 : 0
     * final_score = clamp(positive_score - risk_penalty - thesis_penalty, 0, 100)
     * 
     * For Phase 1, we compute a simplified score based on extraction scores
     */
    public Integer computeStockScore(ExtractedEvent event) {
        if (event == null || event.getBullishScore() == null) {
            return 50; // neutral default
        }
        
        // Phase 1 simplified scoring
        // Use bullish/bearish scores from extraction, adjusted by source quality
        int bullishScore = event.getBullishScore();
        int bearishScore = event.getBearishScore();
        int sourceQualityScore = event.getSourceQualityScore() != null ? event.getSourceQualityScore() : 50;
        
        // Base score is bullish - bearish, adjusted by source quality
        double baseScore = bullishScore - bearishScore;
        double qualityAdjustment = sourceQualityScore / 100.0;
        
        // Compute weighted score
        int stockScore = (int) Math.round((50 + baseScore * 0.5) * qualityAdjustment);
        stockScore = Math.max(0, Math.min(100, stockScore));
        
        log.debug("Computed stock score: {} from bullish={}, bearish={}, sourceQuality={}",
                stockScore, bullishScore, bearishScore, sourceQualityScore);
        
        return stockScore;
    }
    
    /**
     * Generate recommendation based on stock score
     * BUY: score >= 80
     * WATCH: score 60-79
     * HOLD: score 40-59
     * AVOID: score < 40
     */
    public String generateRecommendation(Integer stockScore) {
        if (stockScore == null) {
            return "HOLD";
        }
        
        if (stockScore >= 80) {
            return "BUY";
        } else if (stockScore >= 60) {
            return "WATCH";
        } else if (stockScore >= 40) {
            return "HOLD";
        } else {
            return "AVOID";
        }
    }
}
