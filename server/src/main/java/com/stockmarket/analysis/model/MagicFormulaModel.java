package com.stockmarket.analysis.model;

import com.stockmarket.analysis.AnalysisModel;
import com.stockmarket.analysis.MetricSnapshot;
import com.stockmarket.analysis.StockScore;
import com.stockmarket.model.enums.AnalysisModelType;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.stream.*;

/**
 * Joel Greenblatt's Magic Formula.
 * Ranks stocks by Earnings Yield (EY) and Return on Capital (ROC) separately,
 * then combines ranks (lower combined rank = better stock).
 */
@Slf4j
public class MagicFormulaModel implements AnalysisModel {

    @Override
    public AnalysisModelType getType() { return AnalysisModelType.MAGIC_FORMULA; }

    @Override
    public List<StockScore> analyze(List<MetricSnapshot> snapshots, Map<String, Object> params) {
        final int n = snapshots.size();
        final double[] eyValues  = new double[n];
        final double[] rocValues = new double[n];
        final String[] eySources = new String[n];
        final String[] rocSources = new String[n];

        for (int i = 0; i < n; i++) {
            final MetricSnapshot s = snapshots.get(i);
            eyValues[i]   = Double.NaN;
            rocValues[i]  = Double.NaN;
            eySources[i]  = "N/A";
            rocSources[i] = "N/A";

            // Earnings Yield = EBIT / (MarketCap + TotalLiabilities), fallback: 1/PE
            final Double ebit             = s.getByName("EBIT");
            final Double marketCap        = s.getByName("MARKET_CAP");
            final Double totalLiabilities = s.getByName("TOTAL_LIABILITIES");
            if (ebit != null && marketCap != null && totalLiabilities != null
                    && (marketCap + totalLiabilities) > 1e-9) {
                eyValues[i]  = ebit / (marketCap + totalLiabilities);
                eySources[i] = "EBIT_EV";
            } else {
                final Double pe = s.getByName("PE");
                if (pe != null && Math.abs(pe) > 1e-9) {
                    eyValues[i]  = 1.0 / pe;
                    eySources[i] = "1/PE";
                }
            }

            // Return on Capital = ROCE, fallback: EBIT / (WorkingCapital + TotalAssets - TotalLiabilities)
            final Double roce = s.getByName("ROCE");
            if (roce != null) {
                rocValues[i]  = roce;
                rocSources[i] = "ROCE";
            } else if (ebit != null && s.getByName("WORKING_CAPITAL") != null
                    && s.getByName("TOTAL_ASSETS") != null && totalLiabilities != null) {
                final double denom = s.getByName("WORKING_CAPITAL") + s.getByName("TOTAL_ASSETS") - totalLiabilities;
                if (Math.abs(denom) > 1e-9) {
                    rocValues[i]  = ebit / denom;
                    rocSources[i] = "EBIT_CAPITAL";
                }
            }
        }

        // Rank EY descending (higher EY → lower rank number = better)
        final double[] eyRanks  = rankDescending(eyValues,  n);
        final double[] rocRanks = rankDescending(rocValues, n);

        // Combined rank = EY_RANK + ROC_RANK (lower is better)
        final double[] combined = new double[n];
        double maxCombined = Double.NEGATIVE_INFINITY;
        double minCombined = Double.POSITIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            combined[i] = eyRanks[i] + rocRanks[i];
            if (combined[i] < minCombined) minCombined = combined[i];
            if (combined[i] > maxCombined) maxCombined = combined[i];
        }

        // Normalize to [0, 1] inverted (lower combined rank = higher score)
        final double range = maxCombined - minCombined;
        final List<StockScore> scores = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            final MetricSnapshot s = snapshots.get(i);
            final double score = range < 1e-9 ? 0.5 : (maxCombined - combined[i]) / range;

            final Map<String, Double> breakdown = new LinkedHashMap<>();
            if (!Double.isNaN(eyValues[i]))  breakdown.put("EY_RAW",  eyValues[i]);
            if (!Double.isNaN(rocValues[i])) breakdown.put("ROC_RAW", rocValues[i]);
            breakdown.put("EY_RANK",       eyRanks[i]);
            breakdown.put("ROC_RANK",      rocRanks[i]);
            breakdown.put("COMBINED_RANK", combined[i]);

            final Map<String, Double> rawMetrics = new LinkedHashMap<>();
            rawMetrics.put("EBIT",             s.getByName("EBIT"));
            rawMetrics.put("MARKET_CAP",       s.getByName("MARKET_CAP"));
            rawMetrics.put("TOTAL_LIABILITIES",s.getByName("TOTAL_LIABILITIES"));
            rawMetrics.put("ROCE",             s.getByName("ROCE"));
            rawMetrics.put("PE",               s.getByName("PE"));

            scores.add(new StockScore(s.symbol(), s.exchange(), score, 0, breakdown, rawMetrics));
        }

        scores.sort(Comparator.comparingDouble(StockScore::score).reversed());
        final List<StockScore> ranked = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            final StockScore sc = scores.get(i);
            ranked.add(new StockScore(sc.symbol(), sc.exchange(), sc.score(), i + 1, sc.breakdown(), sc.rawMetrics()));
        }
        return ranked;
    }

    /** Assigns ranks 1..validCount (best=1) for descending ordering; NaN entries get rank n+1. */
    private static double[] rankDescending(double[] values, int n) {
        final double[] ranks = new double[n];
        Arrays.fill(ranks, n + 1.0);

        final List<Integer> validIdx = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (!Double.isNaN(values[i])) validIdx.add(i);
        }
        validIdx.sort((a, b) -> Double.compare(values[b], values[a])); // descending
        for (int r = 0; r < validIdx.size(); r++) {
            ranks[validIdx.get(r)] = r + 1.0;
        }
        return ranks;
    }
}
