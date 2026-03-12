package com.stockmarket.model.dto.response;

import com.stockmarket.model.enums.Exchange;
import com.stockmarket.model.enums.ScreenerIndicator;
import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenerResultResponse {
    private List<ScreenerMatch> matches;
    private int totalScanned;
    private int totalMatched;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScreenerMatch {
        private String symbol;
        private Exchange exchange;
        private Map<ScreenerIndicator, Double> metricValues;
        private List<String> passedFilters;
    }
}
