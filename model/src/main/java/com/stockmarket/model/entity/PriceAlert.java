package com.stockmarket.model.entity;

import com.stockmarket.model.enums.AlertDirection;
import com.stockmarket.model.enums.AlertStatus;
import com.stockmarket.model.enums.Exchange;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceAlert {
    private String id;
    private String userId;
    private String symbol;
    private Exchange exchange;
    private BigDecimal targetPrice;
    private AlertDirection direction;
    private AlertStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime triggeredAt;
}
