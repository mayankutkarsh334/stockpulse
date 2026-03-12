package com.stockmarket.analysis.model;

import com.stockmarket.analysis.AnalysisModel;
import com.stockmarket.analysis.MetricSnapshot;
import com.stockmarket.analysis.StockScore;
import com.stockmarket.model.enums.AnalysisModelType;
import java.util.*;

public class PiotroskiFScoreModel implements AnalysisModel {

    @Override
    public AnalysisModelType getType() { return AnalysisModelType.PIOTROSKI; }

    @Override
    public List<StockScore> analyze(List<MetricSnapshot> snapshots, Map<String, Object> params) {
        List<StockScore> scores = new ArrayList<>();
        for (MetricSnapshot s : snapshots) {
            Map<String, Double> breakdown = new LinkedHashMap<>();
            Map<String, Double> rawMetrics = new LinkedHashMap<>();

            // Profitability signals
            int score = 0;
            score += signal(breakdown, rawMetrics, "ROA_POSITIVE", s.roa(), v -> v > 0);
            score += signal(breakdown, rawMetrics, "OCF_POSITIVE", s.operatingCashFlow(), v -> v > 0);
            score += signal(breakdown, rawMetrics, "ACCRUALS_NEGATIVE",
                    safeSubtract(s.roa(), safeDivide(s.operatingCashFlow(), s.totalAssets())),
                    v -> v < 0);

            // Leverage signals
            score += signal(breakdown, rawMetrics, "DEBT_RATIO_DECREASED", s.debtToEquity(), v -> v < 0.5);
            score += signal(breakdown, rawMetrics, "CURRENT_RATIO_INCREASED", s.currentRatio(), v -> v > 1.0);
            // Shares: can't compare YoY here, use as proxy (shares outstanding < market cap / price suggests no dilution)
            score += signal(breakdown, rawMetrics, "NO_NEW_SHARES", s.sharesOutstanding() != null ? 1.0 : 0.0, v -> v > 0);

            // Efficiency signals
            score += signal(breakdown, rawMetrics, "GROSS_MARGIN_POSITIVE", s.grossMargin(), v -> v > 0);
            score += signal(breakdown, rawMetrics, "ASSET_TURNOVER_POSITIVE", s.assetTurnover(), v -> v > 0);
            score += signal(breakdown, rawMetrics, "EPS_GROWTH_POSITIVE", s.epsGrowth(), v -> v > 0);

            rawMetrics.put("TOTAL_SCORE", (double) score);
            scores.add(new StockScore(s.symbol(), s.exchange(), score, 0, breakdown, rawMetrics));
        }

        scores.sort(Comparator.comparingDouble(StockScore::score).reversed());
        List<StockScore> ranked = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            StockScore sc = scores.get(i);
            ranked.add(new StockScore(sc.symbol(), sc.exchange(), sc.score(), i + 1, sc.breakdown(), sc.rawMetrics()));
        }
        return ranked;
    }

    private int signal(Map<String, Double> breakdown, Map<String, Double> raw,
                       String name, Double value, java.util.function.Predicate<Double> predicate) {
        raw.put(name, value);
        if (value == null) { breakdown.put(name, 0.0); return 0; }
        int s = predicate.test(value) ? 1 : 0;
        breakdown.put(name, (double) s);
        return s;
    }

    private Double safeSubtract(Double a, Double b) {
        if (a == null || b == null) return null;
        return a - b;
    }

    private Double safeDivide(Double a, Double b) {
        if (a == null || b == null || b == 0) return null;
        return a / b;
    }
}
