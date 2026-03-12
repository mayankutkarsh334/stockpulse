package com.stockmarket.model.entity;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Watchlist {
    private String id;
    private String userId;
    private String name;
    private LocalDateTime createdAt;
    private List<WatchlistSymbol> symbols;
}
