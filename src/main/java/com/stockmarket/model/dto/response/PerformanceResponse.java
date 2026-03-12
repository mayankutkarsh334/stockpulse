package com.stockmarket.model.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceResponse {
    private String portfolioId;
    private List<DataPoint> timeSeries;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataPoint {
        private LocalDate date;
        private BigDecimal portfolioValue;
        private BigDecimal invested;
        private BigDecimal pnl;
    }
}
