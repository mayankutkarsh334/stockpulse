package com.stockmarket.screener.preset;

import com.stockmarket.model.dto.request.ScreenerScanRequest;
import com.stockmarket.model.enums.ScreenerIndicator;
import java.util.List;

public class HighPEGrowthPreset implements ScreenerPreset {
    @Override public String getName() { return "HIGH_PE_GROWTH"; }
    @Override public String getDescription() { return "High growth stocks with PE > 20 and EPS growth > 15%"; }
    @Override public List<ScreenerScanRequest.FilterCriteria> getFilters() {
        return List.of(
                new ScreenerScanRequest.FilterCriteria(ScreenerIndicator.PE, "GT", 20.0),
                new ScreenerScanRequest.FilterCriteria(ScreenerIndicator.EPS_GROWTH, "GT", 0.15)
        );
    }
}
