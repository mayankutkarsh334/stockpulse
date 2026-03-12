package com.stockmarket.analysis;

import com.stockmarket.model.enums.AnalysisModelType;
import java.util.List;
import java.util.Map;

public interface AnalysisModel {
    AnalysisModelType getType();
    List<StockScore> analyze(List<MetricSnapshot> snapshots, Map<String, Object> params);
}
