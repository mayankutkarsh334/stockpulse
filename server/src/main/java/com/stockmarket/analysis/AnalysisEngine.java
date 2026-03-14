package com.stockmarket.analysis;

import com.stockmarket.analysis.model.*;
import com.stockmarket.model.enums.AnalysisModelType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Slf4j
@Singleton
public class AnalysisEngine {

    private final Map<AnalysisModelType, AnalysisModel> models = new EnumMap<>(AnalysisModelType.class);

    @Inject
    public AnalysisEngine() {
        models.put(AnalysisModelType.WEIGHTED_SCORE, new WeightedScoringModel());
        models.put(AnalysisModelType.PIOTROSKI, new PiotroskiFScoreModel());
        models.put(AnalysisModelType.ALTMAN_Z, new AltmanZScoreModel());
        models.put(AnalysisModelType.RELATIVE, new RelativeComparisonModel());
        models.put(AnalysisModelType.CUSTOM, new CustomFormulaModel());
    }

    public Set<String> requiredMetrics(AnalysisModelType modelType, Map<String, Object> params) {
        AnalysisModel model = models.get(modelType);
        if (model == null) {
            throw new IllegalArgumentException("Unknown analysis model: " + modelType);
        }
        return model.requiredMetrics(params != null ? params : Map.of());
    }

    public List<StockScore> analyze(AnalysisModelType modelType, List<MetricSnapshot> snapshots,
                                     Map<String, Object> params) {
        AnalysisModel model = models.get(modelType);
        if (model == null) {
            throw new IllegalArgumentException("Unknown analysis model: " + modelType);
        }
        log.info("Running {} analysis on {} stocks", modelType, snapshots.size());
        return model.analyze(snapshots, params != null ? params : Map.of());
    }
}
