package com.stockmarket.analysis.model;

import com.stockmarket.analysis.AnalysisModel;
import com.stockmarket.analysis.MetricSnapshot;
import com.stockmarket.analysis.StockScore;
import com.stockmarket.model.enums.AnalysisModelType;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.stream.*;

@Slf4j
public class WeightedScoringModel implements AnalysisModel {

    private static final Set<String> DEFAULT_METRICS =
            Set.of("PE", "EPS_GROWTH", "ROA", "CURRENT_RATIO", "RSI_14");

    @Override
    public AnalysisModelType getType() { return AnalysisModelType.WEIGHTED_SCORE; }

    @Override
    public Set<String> requiredMetrics(Map<String, Object> params) {
        if (params != null && params.containsKey("weights")) {
            @SuppressWarnings("unchecked")
            Map<String, ?> w = (Map<String, ?>) params.get("weights");
            Set<String> keys = new HashSet<>();
            for (String k : w.keySet()) keys.add(k.toUpperCase());
            return keys;
        }
        return DEFAULT_METRICS;
    }

    @Override
    public List<StockScore> analyze(List<MetricSnapshot> snapshots, Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        Map<String, Object> weightsRaw = (Map<String, Object>) params.getOrDefault("weights", Map.of());
        Map<String, Double> weights = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : weightsRaw.entrySet()) {
            weights.put(e.getKey().toUpperCase(), ((Number) e.getValue()).doubleValue());
        }

        if (weights.isEmpty()) {
            // Default equal weights for key metrics
            weights = Map.of("PE", 0.2, "EPS_GROWTH", 0.3, "ROA", 0.2, "CURRENT_RATIO", 0.15, "RSI_14", 0.15);
        }

        // Collect all values per metric for normalization
        Map<String, List<Double>> allValues = new LinkedHashMap<>();
        for (String metric : weights.keySet()) {
            List<Double> vals = new ArrayList<>();
            for (MetricSnapshot s : snapshots) {
                Double v = s.getByName(metric);
                if (v != null) vals.add(v);
            }
            allValues.put(metric, vals);
        }

        // Compute min/max per metric
        Map<String, Double> mins = new HashMap<>();
        Map<String, Double> maxs = new HashMap<>();
        for (Map.Entry<String, List<Double>> e : allValues.entrySet()) {
            List<Double> vals = e.getValue();
            if (vals.isEmpty()) { mins.put(e.getKey(), 0.0); maxs.put(e.getKey(), 1.0); }
            else {
                mins.put(e.getKey(), Collections.min(vals));
                maxs.put(e.getKey(), Collections.max(vals));
            }
        }

        // Extract lower-is-better metrics
        @SuppressWarnings("unchecked")
        List<String> invertList = (List<String>) params.getOrDefault("invertMetrics", List.of());
        final Set<String> invertMetrics = invertList.stream()
                .map(String::toUpperCase)
                .collect(java.util.stream.Collectors.toSet());

        // Score each stock
        List<StockScore> scores = new ArrayList<>();
        for (MetricSnapshot snapshot : snapshots) {
            Map<String, Double> breakdown = new LinkedHashMap<>();
            Map<String, Double> rawMetrics = new LinkedHashMap<>();
            double totalScore = 0.0;
            double totalWeight = 0.0;

            for (Map.Entry<String, Double> we : weights.entrySet()) {
                String metric = we.getKey();
                double weight = we.getValue();
                Double rawVal = snapshot.getByName(metric);
                rawMetrics.put(metric, rawVal);
                if (rawVal == null) continue;

                double min = mins.get(metric);
                double max = maxs.get(metric);
                double normalized = (max - min) < 1e-9 ? 0.5 : (rawVal - min) / (max - min);
                if (invertMetrics.contains(metric)) normalized = 1.0 - normalized;
                breakdown.put(metric, normalized * weight);
                totalScore += normalized * weight;
                totalWeight += weight;
            }

            double finalScore = totalWeight > 0 ? totalScore / totalWeight : 0.0;
            scores.add(new StockScore(snapshot.symbol(), snapshot.exchange(), finalScore, 0, breakdown, rawMetrics));
        }

        // Rank descending
        scores.sort(Comparator.comparingDouble(StockScore::score).reversed());
        List<StockScore> ranked = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            StockScore s = scores.get(i);
            ranked.add(new StockScore(s.symbol(), s.exchange(), s.score(), i + 1, s.breakdown(), s.rawMetrics()));
        }
        return ranked;
    }
}
