package com.stockmarket.analysis;

import com.stockmarket.model.enums.AnalysisModelType;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AnalysisModel {
    AnalysisModelType getType();
    List<StockScore> analyze(List<MetricSnapshot> snapshots, Map<String, Object> params);

    default Set<String> requiredMetrics(Map<String, Object> params) {
        return Collections.emptySet(); // empty = fetch everything (safe default)
    }
}
