package com.stockmarket.service;

import com.stockmarket.analysis.AnalysisEngine;
import com.stockmarket.analysis.MetricSnapshot;
import com.stockmarket.analysis.StockScore;
import com.stockmarket.csv.CsvStockParser;
import com.stockmarket.model.dto.request.CsvAnalysisRequest;
import com.stockmarket.model.dto.response.CsvAnalysisResultResponse;
import com.stockmarket.model.enums.AnalysisModelType;
import com.stockmarket.model.enums.Exchange;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class CsvAnalysisService {

    // Default weights tuned for Indian equity screener data
    private static final Map<String, Double> DEFAULT_WEIGHTS = Map.of(
            "ROCE",                      0.20,
            "ROE",                       0.15,
            "EPS_GROWTH",                0.15,
            "PE",                        0.15,
            "DEBT_TO_EQUITY",            0.10,
            "REVENUE_GROWTH",            0.10,
            "RSI_14",                    0.08,
            "PB_RATIO",                  0.07
    );

    // Metrics where a lower value is preferable
    private static final List<String> DEFAULT_INVERT_METRICS =
            List.of("PE", "DEBT_TO_EQUITY", "PB_RATIO", "PLEDGED_PROMOTER_HOLDINGS", "FORWARD_PE");

    private final AnalysisEngine engine;
    private final CsvStockParser parser;

    @Inject
    public CsvAnalysisService(final AnalysisEngine engine) {
        this.engine = engine;
        this.parser = new CsvStockParser();
    }

    public CsvAnalysisResultResponse analyze(final CsvAnalysisRequest request) {
        // Determine available metric keys from CSV header
        final Set<String> availableKeys = parser.parseAvailableMetricKeys(request.getCsvContent());

        final AnalysisModelType modelType = request.getModelType() != null
                ? request.getModelType() : AnalysisModelType.WEIGHTED_SCORE;

        // Validate requested weights only for WEIGHTED_SCORE
        if (modelType == AnalysisModelType.WEIGHTED_SCORE) {
            final Map<String, Double> weights = request.getWeights() != null ? request.getWeights() : DEFAULT_WEIGHTS;
            final List<String> missingMetrics = weights.keySet().stream()
                    .filter(k -> !availableKeys.contains(k))
                    .sorted()
                    .collect(Collectors.toList());
            if (!missingMetrics.isEmpty()) {
                throw new WebApplicationException(
                        Response.status(422)
                                .type(MediaType.APPLICATION_JSON)
                                .entity(Map.of("error", "Metrics not in CSV: " + missingMetrics))
                                .build());
            }
        }

        final var parsed = parser.parse(request.getCsvContent(), Exchange.NSE);

        if (parsed.isEmpty()) {
            return CsvAnalysisResultResponse.builder()
                    .rankings(List.of())
                    .availableMetrics(new ArrayList<>(availableKeys))
                    .computedAtMs(System.currentTimeMillis())
                    .totalStocksAnalyzed(0)
                    .build();
        }

        final List<MetricSnapshot> snapshots = parsed.stream()
                .map(CsvStockParser.ParsedCsvStock::snapshot)
                .collect(Collectors.toList());

        // Build model-specific params
        final Map<String, Object> params = new LinkedHashMap<>();
        switch (modelType) {
            case WEIGHTED_SCORE -> {
                final Map<String, Double> weights = request.getWeights() != null ? request.getWeights() : DEFAULT_WEIGHTS;
                params.put("weights", weights);
                final List<String> invertMetrics = request.getInvertMetrics() != null
                        ? request.getInvertMetrics() : DEFAULT_INVERT_METRICS;
                params.put("invertMetrics", invertMetrics);
            }
            case RELATIVE -> params.put("metrics", new ArrayList<>(availableKeys));
            case CUSTOM -> params.put("formula",
                    request.getFormula() != null ? request.getFormula() : "EPS_GROWTH");
            // PIOTROSKI, ALTMAN_Z, MAGIC_FORMULA, MULTI_FACTOR: no params needed
            case MAGIC_FORMULA, MULTI_FACTOR -> { }
            default -> { }
        }

        final List<StockScore> scores = engine.analyze(modelType, snapshots, params);

        // Build company name lookup
        final Map<String, String> nameMap = new LinkedHashMap<>();
        final Map<String, String> sectorMap = new LinkedHashMap<>();
        final Map<String, Double> priceMap = new LinkedHashMap<>();
        for (var p : parsed) {
            nameMap.put(p.snapshot().symbol(), p.companyName());
            sectorMap.put(p.snapshot().symbol(), p.sector());
            priceMap.put(p.snapshot().symbol(), p.snapshot().closePrice());
        }

        // Build ranked list
        final List<CsvAnalysisResultResponse.CsvRankedStock> rankings = scores.stream()
                .map(s -> CsvAnalysisResultResponse.CsvRankedStock.builder()
                        .symbol(s.symbol())
                        .companyName(nameMap.getOrDefault(s.symbol(), s.symbol()))
                        .sector(sectorMap.get(s.symbol()))
                        .score(s.score())
                        .rank(s.rank())
                        .breakdown(s.breakdown())
                        .rawMetrics(s.rawMetrics())
                        .closePrice(priceMap.get(s.symbol()))
                        .build())
                .collect(Collectors.toList());

        // Build investment recommendation from rank #1
        final CsvAnalysisResultResponse.InvestmentRecommendation recommendation =
                buildRecommendation(scores, nameMap, priceMap, modelType);

        return CsvAnalysisResultResponse.builder()
                .rankings(rankings)
                .recommendation(recommendation)
                .availableMetrics(new ArrayList<>(availableKeys))
                .computedAtMs(System.currentTimeMillis())
                .totalStocksAnalyzed(parsed.size())
                .build();
    }

    private CsvAnalysisResultResponse.InvestmentRecommendation buildRecommendation(
            final List<StockScore> scores,
            final Map<String, String> nameMap,
            final Map<String, Double> priceMap,
            final AnalysisModelType modelType) {

        if (scores.isEmpty()) return null;

        final StockScore top = scores.get(0);
        final Double price = priceMap.get(top.symbol());

        final String confidence = confidenceTier(modelType, top.score());
        final String rationale = buildRationale(top, nameMap);

        return CsvAnalysisResultResponse.InvestmentRecommendation.builder()
                .symbol(top.symbol())
                .companyName(nameMap.getOrDefault(top.symbol(), top.symbol()))
                .rank(top.rank())
                .score(top.score())
                .confidenceTier(confidence)
                .closePrice(price)
                .rationale(rationale)
                .build();
    }

    private String confidenceTier(final AnalysisModelType t, final double score) {
        return switch (t) {
            case PIOTROSKI    -> score >= 7 ? "HIGH" : score >= 4 ? "MEDIUM" : "LOW";
            case ALTMAN_Z     -> score > 2.99 ? "HIGH" : score > 1.81 ? "MEDIUM" : "LOW";
            case RELATIVE,
                 MULTI_FACTOR -> score >= 1.0 ? "HIGH" : score >= 0.0 ? "MEDIUM" : "LOW";
            case CUSTOM       -> "MEDIUM";
            default           -> score >= 0.65 ? "HIGH" : score >= 0.35 ? "MEDIUM" : "LOW";
        };
    }

    private String buildRationale(final StockScore top, final Map<String, String> nameMap) {
        if (top.breakdown() == null || top.breakdown().isEmpty()) {
            return top.symbol() + " ranked #1 with the highest composite score.";
        }
        // Top 2 contributing metrics by weighted score
        final List<Map.Entry<String, Double>> topMetrics = top.breakdown().entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(2)
                .collect(Collectors.toList());

        final StringBuilder sb = new StringBuilder();
        sb.append(nameMap.getOrDefault(top.symbol(), top.symbol()))
          .append(" leads on ");
        for (int i = 0; i < topMetrics.size(); i++) {
            String metric = topMetrics.get(i).getKey();
            Double raw = top.rawMetrics() != null ? top.rawMetrics().get(metric) : null;
            sb.append(metric.replace('_', ' '));
            if (raw != null) sb.append(String.format(" (%.2f)", raw));
            if (i == 0 && topMetrics.size() > 1) sb.append(" and ");
        }
        sb.append(".");
        return sb.toString();
    }
}
