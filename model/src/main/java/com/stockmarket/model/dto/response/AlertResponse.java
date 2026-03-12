package com.stockmarket.model.dto.response;

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
public class AlertResponse {
    private String id;
    private String userId;
    private String symbol;
    private Exchange exchange;
    private BigDecimal targetPrice;
    private BigDecimal currentPrice;
    private AlertDirection direction;
    private AlertStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime triggeredAt;
}
