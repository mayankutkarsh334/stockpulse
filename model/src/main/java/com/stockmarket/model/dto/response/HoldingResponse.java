package com.stockmarket.model.dto.response;

import com.stockmarket.model.enums.Currency;
import com.stockmarket.model.enums.Exchange;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldingResponse {
    private String id;
    private String symbol;
    private Exchange exchange;
    private BigDecimal quantity;
    private BigDecimal averageBuyPrice;
    private BigDecimal currentPrice;
    private BigDecimal investedValue;
    private BigDecimal currentValue;
    private BigDecimal pnl;
    private BigDecimal pnlPercent;
    private Currency currency;
}
