package com.stockmarket.model.dto.response;

import com.stockmarket.model.enums.Exchange;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistResponse {
    private String id;
    private String userId;
    private String name;
    private List<WatchlistItemResponse> symbols;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WatchlistItemResponse {
        private String symbol;
        private Exchange exchange;
        private BigDecimal currentPrice;
        private BigDecimal change;
        private BigDecimal changePercent;
    }
}
