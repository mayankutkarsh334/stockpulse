package com.stockmarket.analysis.model;

import com.stockmarket.analysis.AnalysisModel;
import com.stockmarket.analysis.MetricSnapshot;
import com.stockmarket.analysis.StockScore;
import com.stockmarket.model.enums.AnalysisModelType;
import java.util.*;

public class AltmanZScoreModel implements AnalysisModel {

    @Override
    public AnalysisModelType getType() { return AnalysisModelType.ALTMAN_Z; }

    @Override
    public List<StockScore> analyze(List<MetricSnapshot> snapshots, Map<String, Object> params) {
        List<StockScore> scores = new ArrayList<>();
        for (MetricSnapshot s : snapshots) {
            Map<String, Double> breakdown = new LinkedHashMap<>();
            Map<String, Double> rawMetrics = new LinkedHashMap<>();

            // X1 = Working Capital / Total Assets
            Double x1 = safeDivide(s.workingCapital(), s.totalAssets());
            // X2 = Retained Earnings / Total Assets
            Double x2 = safeDivide(s.retainedEarnings(), s.totalAssets());
            // X3 = EBIT / Total Assets
            Double x3 = safeDivide(s.ebit(), s.totalAssets());
            // X4 = Market Cap / Total Liabilities
            Double x4 = safeDivide(s.marketCap(), s.totalLiabilities());
            // X5 = Revenue growth proxy (use revenue growth directly as relative measure)
            Double x5 = s.revenueGrowth();

            rawMetrics.put("X1_WORKING_CAPITAL_RATIO", x1);
            rawMetrics.put("X2_RETAINED_EARNINGS_RATIO", x2);
            rawMetrics.put("X3_EBIT_RATIO", x3);
            rawMetrics.put("X4_MARKET_CAP_TO_LIABILITIES", x4);
            rawMetrics.put("X5_REVENUE_GROWTH", x5);

            double z = 0.0;
            if (x1 != null) { breakdown.put("1.2*X1", 1.2 * x1); z += 1.2 * x1; }
            if (x2 != null) { breakdown.put("1.4*X2", 1.4 * x2); z += 1.4 * x2; }
            if (x3 != null) { breakdown.put("3.3*X3", 3.3 * x3); z += 3.3 * x3; }
            if (x4 != null) { breakdown.put("0.6*X4", 0.6 * x4); z += 0.6 * x4; }
            if (x5 != null) { breakdown.put("1.0*X5", x5); z += x5; }

            // Classify
            String zone = z > 2.99 ? "SAFE" : z > 1.81 ? "GREY" : "DISTRESS";
            rawMetrics.put("ZONE_SAFE_3_GREY_2_DISTRESS_1",
                    z > 2.99 ? 3.0 : z > 1.81 ? 2.0 : 1.0);
            breakdown.put("ZONE_" + zone, z);

            scores.add(new StockScore(s.symbol(), s.exchange(), z, 0, breakdown, rawMetrics));
        }

        scores.sort(Comparator.comparingDouble(StockScore::score).reversed());
        List<StockScore> ranked = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            StockScore sc = scores.get(i);
            ranked.add(new StockScore(sc.symbol(), sc.exchange(), sc.score(), i + 1, sc.breakdown(), sc.rawMetrics()));
        }
        return ranked;
    }

    private Double safeDivide(Double a, Double b) {
        if (a == null || b == null || b == 0) return null;
        return a / b;
    }
}
