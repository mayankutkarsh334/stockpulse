package com.stockmarket.screener.preset;

import com.stockmarket.model.dto.request.ScreenerScanRequest;
import com.stockmarket.model.enums.ScreenerIndicator;
import java.util.List;

public class LargecapValuePreset implements ScreenerPreset {
    @Override public String getName() { return "LARGECAP_VALUE"; }
    @Override public String getDescription() { return "Large-cap value stocks: Market Cap > 10B, PE < 15, Debt/Equity < 0.5"; }
    @Override public List<ScreenerScanRequest.FilterCriteria> getFilters() {
        return List.of(
                new ScreenerScanRequest.FilterCriteria(ScreenerIndicator.MARKET_CAP, "GT", 10_000_000_000.0),
                new ScreenerScanRequest.FilterCriteria(ScreenerIndicator.PE, "LT", 15.0),
                new ScreenerScanRequest.FilterCriteria(ScreenerIndicator.DEBT_TO_EQUITY, "LT", 0.5)
        );
    }
}
