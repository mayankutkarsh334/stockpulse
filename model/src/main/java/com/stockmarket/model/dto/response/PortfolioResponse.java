package com.stockmarket.model.dto.response;

import com.stockmarket.model.enums.Currency;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponse {
    private String id;
    private String userId;
    private String name;
    private Currency currency;
    private BigDecimal totalInvested;
    private BigDecimal totalCurrentValue;
    private BigDecimal totalPnl;
    private BigDecimal totalPnlPercent;
    private List<HoldingResponse> holdings;
    private LocalDateTime createdAt;
}
