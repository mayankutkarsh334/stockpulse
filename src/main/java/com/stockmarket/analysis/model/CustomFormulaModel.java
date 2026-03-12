package com.stockmarket.analysis.model;

import com.stockmarket.analysis.AnalysisModel;
import com.stockmarket.analysis.MetricSnapshot;
import com.stockmarket.analysis.StockScore;
import com.stockmarket.analysis.formula.FormulaEvaluator;
import com.stockmarket.analysis.formula.FormulaParser;
import com.stockmarket.analysis.formula.FormulaNode;
import com.stockmarket.model.enums.AnalysisModelType;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Slf4j
public class CustomFormulaModel implements AnalysisModel {

    @Override
    public AnalysisModelType getType() { return AnalysisModelType.CUSTOM; }

    @Override
    public List<StockScore> analyze(List<MetricSnapshot> snapshots, Map<String, Object> params) {
        String formula = (String) params.getOrDefault("formula", "EPS_GROWTH");
        FormulaParser parser = new FormulaParser();
        FormulaNode ast;
        try {
            ast = parser.parse(formula);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid formula: " + formula + " — " + e.getMessage(), e);
        }

        FormulaEvaluator evaluator = new FormulaEvaluator();
        List<StockScore> scores = new ArrayList<>();

        for (MetricSnapshot snapshot : snapshots) {
            Map<String, Double> rawMetrics = new LinkedHashMap<>();
            double score;
            try {
                score = evaluator.evaluate(ast, snapshot);
            } catch (Exception e) {
                log.warn("Formula evaluation failed for {}: {}", snapshot.symbol(), e.getMessage());
                score = Double.NaN;
            }
            rawMetrics.put("FORMULA_RESULT", Double.isNaN(score) ? null : score);
            Map<String, Double> breakdown = Map.of("formula", Double.isNaN(score) ? 0.0 : score);
            scores.add(new StockScore(snapshot.symbol(), snapshot.exchange(),
                    Double.isNaN(score) ? 0.0 : score, 0, breakdown, rawMetrics));
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
