package com.stockmarket.model.entity;

import com.stockmarket.model.enums.Exchange;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistSymbol {
    private String watchlistId;
    private String symbol;
    private Exchange exchange;
    private LocalDateTime addedAt;
}
