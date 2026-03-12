package com.stockmarket.model.entity;

import com.stockmarket.model.enums.Currency;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Portfolio {
    private String id;
    private String userId;
    private String name;
    private Currency currency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
