package com.stockmarket.model.entity;

import com.stockmarket.model.enums.Currency;
import com.stockmarket.model.enums.Exchange;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Holding {
    private String id;
    private String portfolioId;
    private String symbol;
    private Exchange exchange;
    private BigDecimal quantity;
    private BigDecimal averageBuyPrice;
    private Currency currency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
