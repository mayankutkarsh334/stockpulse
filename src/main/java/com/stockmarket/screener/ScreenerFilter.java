package com.stockmarket.screener;

import com.stockmarket.model.enums.ScreenerIndicator;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenerFilter {
    private ScreenerIndicator indicator;
    private String operator;
    private Double value;
}
