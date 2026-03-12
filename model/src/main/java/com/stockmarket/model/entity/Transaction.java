package com.stockmarket.model.entity;

import com.stockmarket.model.enums.TransactionType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private String id;
    private String holdingId;
    private String portfolioId;
    private TransactionType type;
    private BigDecimal quantity;
    private BigDecimal price;
    private String notes;
    private LocalDateTime transactedAt;
    private LocalDateTime createdAt;
}
