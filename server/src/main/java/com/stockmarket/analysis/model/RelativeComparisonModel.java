package com.stockmarket.analysis.model;

import com.stockmarket.analysis.AnalysisModel;
import com.stockmarket.analysis.MetricSnapshot;
import com.stockmarket.analysis.StockScore;
import com.stockmarket.model.enums.AnalysisModelType;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.stream.*;

@Slf4j
public class RelativeComparisonModel implements AnalysisModel {

    private static final List<String> DEFAULT_METRICS =
            List.of("PE", "EPS_GROWTH", "ROA", "DEBT_TO_EQUITY", "CURRENT_RATIO", "GROSS_MARGIN");

    private static final Set<String> DEFAULT_METRICS_SET =
            Set.of("PE", "EPS_GROWTH", "ROA", "DEBT_TO_EQUITY", "CURRENT_RATIO", "GROSS_MARGIN");

    @Override
    public AnalysisModelType getType() { return AnalysisModelType.RELATIVE; }

    @Override
    public Set<String> requiredMetrics(Map<String, Object> params) {
        if (params != null && params.containsKey("metrics")) {
            @SuppressWarnings("unchecked")
            List<String> m = (List<String>) params.get("metrics");
            return new HashSet<>(m);
        }
        return DEFAULT_METRICS_SET;
    }

    @Override
    public List<StockScore> analyze(List<MetricSnapshot> snapshots, Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<String> metrics = params.containsKey("metrics")
                ? (List<String>) params.get("metrics")
                : DEFAULT_METRICS;

        // Group by sector
        Map<String, List<MetricSnapshot>> bySector = snapshots.stream()
                .collect(Collectors.groupingBy(s -> s.sector() != null ? s.sector() : "UNKNOWN"));

        // Compute sector means and stds per metric
        Map<String, Map<String, double[]>> sectorStats = new HashMap<>();
        for (Map.Entry<String, List<MetricSnapshot>> entry : bySector.entrySet()) {
            Map<String, double[]> metricStats = new HashMap<>();
            for (String metric : metrics) {
                List<Double> vals = entry.getValue().stream()
                        .map(s -> s.getByName(metric))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                if (vals.isEmpty()) { metricStats.put(metric, new double[]{0, 1}); continue; }
                double mean = vals.stream().mapToDouble(d -> d).average().orElse(0);
                double std = Math.sqrt(vals.stream().mapToDouble(d -> (d - mean) * (d - mean)).average().orElse(1));
                if (std < 1e-9) std = 1.0;
                metricStats.put(metric, new double[]{mean, std});
            }
            sectorStats.put(entry.getKey(), metricStats);
        }

        List<StockScore> scores = new ArrayList<>();
        for (MetricSnapshot s : snapshots) {
            String sector = s.sector() != null ? s.sector() : "UNKNOWN";
            Map<String, double[]> metricStats = sectorStats.getOrDefault(sector, Map.of());
            Map<String, Double> breakdown = new LinkedHashMap<>();
            Map<String, Double> rawMetrics = new LinkedHashMap<>();
            double totalZScore = 0.0;
            int count = 0;

            for (String metric : metrics) {
                Double val = s.getByName(metric);
                rawMetrics.put(metric, val);
                if (val == null) continue;
                double[] stats = metricStats.getOrDefault(metric, new double[]{0, 1});
                double zScore = (val - stats[0]) / stats[1];
                breakdown.put(metric + "_ZSCORE", zScore);
                totalZScore += zScore;
                count++;
            }

            double finalScore = count > 0 ? totalZScore / count : 0.0;
            scores.add(new StockScore(s.symbol(), s.exchange(), finalScore, 0, breakdown, rawMetrics));
        }

        scores.sort(Comparator.comparingDouble(StockScore::score).reversed());
        List<StockScore> ranked = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            StockScore sc = scores.get(i);
            ranked.add(new StockScore(sc.symbol(), sc.exchange(), sc.score(), i + 1, sc.breakdown(), sc.rawMetrics()));
        }
        return ranked;
    }
}
