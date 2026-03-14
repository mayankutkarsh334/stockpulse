package com.stockmarket.analysis.model;

import com.stockmarket.analysis.AnalysisModel;
import com.stockmarket.analysis.MetricSnapshot;
import com.stockmarket.analysis.StockScore;
import com.stockmarket.model.enums.AnalysisModelType;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.stream.*;

/**
 * Fama-French inspired multi-factor composite model.
 * Combines Quality, Value, Momentum, and Growth factors via Z-scores.
 */
@Slf4j
public class MultiFactorCompositeModel implements AnalysisModel {

    private static final List<String> QUALITY_METRICS  = List.of("ROCE", "ROE");
    private static final List<String> VALUE_METRICS    = List.of("PE", "PB_RATIO");
    private static final List<String> MOMENTUM_METRICS = List.of("ONE_YEAR_RETURN", "SIX_MONTH_RETURN");
    private static final List<String> GROWTH_METRICS   = List.of("EPS_GROWTH", "REVENUE_GROWTH");
    // Lower is better for these — negate their Z-scores
    private static final Set<String>  INVERT_METRICS   = Set.of("PE", "PB_RATIO");

    @Override
    public AnalysisModelType getType() { return AnalysisModelType.MULTI_FACTOR; }

    @Override
    public List<StockScore> analyze(List<MetricSnapshot> snapshots, Map<String, Object> params) {
        final List<String> allMetrics = new ArrayList<>();
        allMetrics.addAll(QUALITY_METRICS);
        allMetrics.addAll(VALUE_METRICS);
        allMetrics.addAll(MOMENTUM_METRICS);
        allMetrics.addAll(GROWTH_METRICS);

        // Compute cross-sectional mean and std for each metric
        final Map<String, Double> means = new HashMap<>();
        final Map<String, Double> stds  = new HashMap<>();
        for (String metric : allMetrics) {
            final List<Double> vals = snapshots.stream()
                    .map(s -> s.getByName(metric))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (vals.isEmpty()) { means.put(metric, 0.0); stds.put(metric, 1.0); continue; }
            final double mean = vals.stream().mapToDouble(d -> d).average().orElse(0);
            double std = Math.sqrt(vals.stream().mapToDouble(d -> (d - mean) * (d - mean)).average().orElse(1));
            if (std < 1e-9) std = 1.0;
            means.put(metric, mean);
            stds.put(metric, std);
        }

        final List<StockScore> scores = new ArrayList<>();
        for (MetricSnapshot s : snapshots) {
            final Map<String, Double> breakdown  = new LinkedHashMap<>();
            final Map<String, Double> rawMetrics = new LinkedHashMap<>();

            // Compute Z-scores; missing → 0.0 (neutral)
            final Map<String, Double> zScores = new HashMap<>();
            for (String metric : allMetrics) {
                final Double val = s.getByName(metric);
                rawMetrics.put(metric, val);
                double z = 0.0;
                if (val != null) {
                    z = (val - means.get(metric)) / stds.get(metric);
                    if (INVERT_METRICS.contains(metric)) z = -z;
                }
                zScores.put(metric, z);
                breakdown.put("Z_" + metric, z);
            }

            // Compute factor scores (null if entire group is missing)
            final Double qualityFactor  = factorScore(QUALITY_METRICS,  zScores, s);
            final Double valueFactor    = factorScore(VALUE_METRICS,    zScores, s);
            final Double momentumFactor = factorScore(MOMENTUM_METRICS, zScores, s);
            final Double growthFactor   = factorScore(GROWTH_METRICS,   zScores, s);

            if (qualityFactor  != null) breakdown.put("QUALITY_FACTOR",  qualityFactor);
            if (valueFactor    != null) breakdown.put("VALUE_FACTOR",     valueFactor);
            if (momentumFactor != null) breakdown.put("MOMENTUM_FACTOR",  momentumFactor);
            if (growthFactor   != null) breakdown.put("GROWTH_FACTOR",    growthFactor);

            // Average available factor scores
            double total = 0;
            int count = 0;
            for (Double f : new Double[]{qualityFactor, valueFactor, momentumFactor, growthFactor}) {
                if (f != null) { total += f; count++; }
            }
            final double finalScore = count > 0 ? total / count : 0.0;

            scores.add(new StockScore(s.symbol(), s.exchange(), finalScore, 0, breakdown, rawMetrics));
        }

        scores.sort(Comparator.comparingDouble(StockScore::score).reversed());
        final List<StockScore> ranked = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            final StockScore sc = scores.get(i);
            ranked.add(new StockScore(sc.symbol(), sc.exchange(), sc.score(), i + 1, sc.breakdown(), sc.rawMetrics()));
        }
        return ranked;
    }

    /**
     * Returns the average Z-score for the group, or null if all metrics in the group
     * are missing from this snapshot (group excluded from the composite average).
     */
    private Double factorScore(List<String> metrics, Map<String, Double> zScores, MetricSnapshot s) {
        final boolean anyReal = metrics.stream().anyMatch(m -> s.getByName(m) != null);
        if (!anyReal) return null;
        return metrics.stream().mapToDouble(m -> zScores.getOrDefault(m, 0.0)).average().orElse(0);
    }
}
