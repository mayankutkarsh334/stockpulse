package com.stockmarket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmarket.analysis.AnalysisEngine;
import com.stockmarket.analysis.MetricFetcher;
import com.stockmarket.analysis.MetricSnapshot;
import com.stockmarket.analysis.StockScore;
import com.stockmarket.client.AlphaVantageClient;
import com.stockmarket.dao.aerospike.FundamentalsCache;
import com.stockmarket.dao.aerospike.TechnicalsCache;
import com.stockmarket.dao.mysql.AnalysisConfigDao;
import com.stockmarket.kafka.producer.AnalysisJobProducer;
import com.stockmarket.model.dto.request.AnalysisRequest;
import com.stockmarket.model.dto.response.AnalysisResultResponse;
import com.stockmarket.model.entity.AnalysisConfig;
import com.stockmarket.model.enums.AnalysisModelType;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class AnalysisService {

    private final FundamentalsCache fundamentalsCache;
    private final TechnicalsCache technicalsCache;
    private final AlphaVantageClient avClient;
    private final AnalysisConfigDao analysisConfigDao;
    private final AnalysisJobProducer analysisJobProducer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnalysisResultResponse runAnalysis(AnalysisRequest req) {
        MetricFetcher fetcher = new MetricFetcher(fundamentalsCache, technicalsCache, avClient);
        List<MetricSnapshot> snapshots = req.getSymbols().stream()
                .map(s -> {
                    try {
                        return fetcher.fetch(s, req.getExchange());
                    } catch (Exception e) {
                        log.warn("Failed to fetch metrics for {}: {}", s, e.getMessage());
                        return MetricSnapshot.empty(s, req.getExchange());
                    }
                })
                .collect(Collectors.toList());

        AnalysisEngine engine = new AnalysisEngine();
        List<StockScore> scores = engine.analyze(req.getModelType(), snapshots, req.getParams());

        List<AnalysisResultResponse.RankedStock> ranked = scores.stream()
                .map(s -> AnalysisResultResponse.RankedStock.builder()
                        .symbol(s.symbol())
                        .exchange(s.exchange())
                        .score(s.score())
                        .rank(s.rank())
                        .breakdown(s.breakdown())
                        .rawMetrics(s.rawMetrics())
                        .build())
                .collect(Collectors.toList());

        return AnalysisResultResponse.builder()
                .modelType(req.getModelType())
                .rankings(ranked)
                .computedAtMs(System.currentTimeMillis())
                .build();
    }

    public AnalysisConfig saveConfig(String userId, String name, AnalysisModelType modelType, Object params) {
        try {
            String paramsJson = objectMapper.writeValueAsString(params);
            AnalysisConfig config = AnalysisConfig.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .name(name)
                    .modelType(modelType)
                    .paramsJson(paramsJson)
                    .build();
            analysisConfigDao.insert(config);
            return config;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save analysis config", e);
        }
    }

    public AnalysisConfig getConfig(String configId) {
        return analysisConfigDao.findById(configId)
                .orElseThrow(() -> new NotFoundException("Analysis config not found: " + configId));
    }

    public List<AnalysisModelType> getAvailableModels() {
        return List.of(AnalysisModelType.values());
    }
}
