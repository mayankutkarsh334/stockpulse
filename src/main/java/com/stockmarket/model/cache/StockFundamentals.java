package com.stockmarket.model.cache;

import com.stockmarket.model.enums.Exchange;
import lombok.*;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockFundamentals {
    private String symbol;
    private Exchange exchange;
    private String sector;
    private String industry;
    private String description;
    private Double pe;
    private Double eps;
    private Double epsGrowth;
    private Double revenueGrowth;
    private Long marketCap;
    private Double dividendYield;
    private Double debtToEquity;
    private Double currentRatio;
    private Double roa;
    private Double grossMargin;
    private Double assetTurnover;
    private Double retainedEarnings;
    private Double ebit;
    private Double workingCapital;
    private Double totalAssets;
    private Double totalLiabilities;
    private Double operatingCashFlow;
    private Long sharesOutstanding;
    private Double high52w;
    private Double low52w;
    private Instant fetchedAt;
}
