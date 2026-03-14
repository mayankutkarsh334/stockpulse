package com.stockmarket.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvAnalysisResultResponse {
    private List<CsvRankedStock> rankings;
    private InvestmentRecommendation recommendation;
    private List<String> availableMetrics;
    private long computedAtMs;
    private int totalStocksAnalyzed;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CsvRankedStock {
        private String symbol;
        private String companyName;
        private String sector;
        private double score;
        private int rank;
        private Map<String, Double> breakdown;
        private Map<String, Double> rawMetrics;
        private Double closePrice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvestmentRecommendation {
        private String symbol;
        private String companyName;
        private int rank;
        private double score;
        private String confidenceTier;   // HIGH, MEDIUM, LOW
        private Double closePrice;
        private String rationale;
    }
}
