package com.stockmarket.screener.preset;

import com.stockmarket.model.dto.request.ScreenerScanRequest;
import java.util.List;

public interface ScreenerPreset {
    String getName();
    String getDescription();
    List<ScreenerScanRequest.FilterCriteria> getFilters();
}
