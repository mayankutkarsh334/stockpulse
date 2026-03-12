package com.stockmarket.screener.preset;

import com.stockmarket.model.dto.request.ScreenerScanRequest;
import com.stockmarket.model.enums.ScreenerIndicator;
import java.util.List;

public class OversoldRSIPreset implements ScreenerPreset {
    @Override public String getName() { return "OVERSOLD_RSI"; }
    @Override public String getDescription() { return "Stocks with RSI below 30 (oversold)"; }
    @Override public List<ScreenerScanRequest.FilterCriteria> getFilters() {
        return List.of(new ScreenerScanRequest.FilterCriteria(ScreenerIndicator.RSI_14, "LT", 30.0));
    }
}
