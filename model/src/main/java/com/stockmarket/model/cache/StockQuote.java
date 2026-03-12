package com.stockmarket.model.cache;

import com.stockmarket.model.enums.Exchange;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockQuote {
    private String symbol;
    private Exchange exchange;
    private BigDecimal price;
    private BigDecimal change;
    private BigDecimal changePercent;
    private Long volume;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal previousClose;
    private Instant timestamp;
}
