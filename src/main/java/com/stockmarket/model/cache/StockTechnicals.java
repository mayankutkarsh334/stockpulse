package com.stockmarket.model.cache;

import com.stockmarket.model.enums.Exchange;
import lombok.*;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTechnicals {
    private String symbol;
    private Exchange exchange;
    private Double rsi14;
    private Double macdValue;
    private Double macdSignal;
    private Double macdHistogram;
    private Double sma20;
    private Double sma50;
    private Double sma200;
    private Double currentPrice;
    private Double volume;
    private Double avgVolume;
    private Instant fetchedAt;
}
