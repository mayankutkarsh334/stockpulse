package com.stockmarket.model.dto.request;

import com.stockmarket.model.enums.Exchange;
import com.stockmarket.model.enums.ScreenerIndicator;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScreenerScanRequest {
    @NotNull private List<String> symbols;
    @NotNull private Exchange exchange;
    private List<FilterCriteria> filters;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilterCriteria {
        private ScreenerIndicator indicator;
        private String operator; // GT, LT, GTE, LTE, EQ
        private Double value;
    }
}
