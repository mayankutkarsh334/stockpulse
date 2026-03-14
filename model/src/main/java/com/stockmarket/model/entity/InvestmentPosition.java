package com.stockmarket.model.entity;

import com.stockmarket.model.enums.AnalysisModelType;
import com.stockmarket.model.enums.CloseReason;
import com.stockmarket.model.enums.Exchange;
import com.stockmarket.model.enums.PositionStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentPosition {
    private String id;
    private String symbol;
    private String companyName;
    private String sector;
    private Exchange exchange;
    private BigDecimal entryPrice;
    private LocalDate entryDate;
    private BigDecimal quantity;
    private BigDecimal investedAmount;
    private BigDecimal currentStopLoss;
    private BigDecimal highWaterMarkPrice;
    private String pickedMonth;         // YYYY-MM
    private AnalysisModelType modelType;
    private PositionStatus status;
    private LocalDateTime closedAt;
    private BigDecimal closePrice;
    private BigDecimal closePnl;
    private CloseReason closeReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
