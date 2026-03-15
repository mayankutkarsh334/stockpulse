package com.stockmarket.model.dto.response;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoffeeCanScreenResponse {
    private List<CoffeeCanStock> stocks;
    private int totalScanned;
    private int totalTier1;
    private int totalTier2;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoffeeCanStock {
        private String symbol;
        private String companyName;
        private Double roce;
        private Double debtToEquity;
        private Double revenueGrowth;
        private Double pledgedPromoterHoldings;
        private Double pbRatio;
        private Double marketCap;
        private String tier;
        private List<String> failReasons;
    }
}
