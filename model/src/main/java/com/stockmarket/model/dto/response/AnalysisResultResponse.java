package com.stockmarket.model.dto.response;

import com.stockmarket.model.enums.AnalysisModelType;
import com.stockmarket.model.enums.Exchange;
import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResultResponse {
    private AnalysisModelType modelType;
    private List<RankedStock> rankings;
    private long computedAtMs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RankedStock {
        private String symbol;
        private Exchange exchange;
        private double score;
        private int rank;
        private Map<String, Double> breakdown;
        private Map<String, Double> rawMetrics;
    }
}
