package com.stockmarket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmarket.analysis.AnalysisEngine;
import com.stockmarket.analysis.MetricFetcher;
import com.stockmarket.analysis.MetricSnapshot;
import com.stockmarket.client.AlphaVantageClient;
import com.stockmarket.dao.aerospike.FundamentalsCache;
import com.stockmarket.dao.aerospike.TechnicalsCache;
import com.stockmarket.dao.mysql.AnalysisConfigDao;
import com.stockmarket.kafka.producer.AnalysisJobProducer;
import com.stockmarket.model.dto.request.AnalysisRequest;
import com.stockmarket.model.dto.response.AnalysisResultResponse;
import com.stockmarket.model.entity.AnalysisConfig;
import com.stockmarket.model.enums.AnalysisModelType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class AnalysisService {

    private final FundamentalsCache fundamentalsCache;
    private final TechnicalsCache technicalsCache;
    private final AlphaVantageClient avClient;
    private final AnalysisConfigDao analysisConfigDao;
    private final AnalysisJobProducer analysisJobProducer;
    private final ObjectMapper objectMapper;

    @Inject
    public AnalysisService(final FundamentalsCache fundamentalsCache,
                           final TechnicalsCache technicalsCache,
                           final AlphaVantageClient avClient,
                           final AnalysisConfigDao analysisConfigDao,
                           final AnalysisJobProducer analysisJobProducer,
                           final ObjectMapper objectMapper) {
        this.fundamentalsCache = fundamentalsCache;
        this.technicalsCache = technicalsCache;
        this.avClient = avClient;
        this.analysisConfigDao = analysisConfigDao;
        this.analysisJobProducer = analysisJobProducer;
        this.objectMapper = objectMapper;
    }

    public AnalysisResultResponse runAnalysis(final AnalysisRequest req) {
        final var fetcher = new MetricFetcher(fundamentalsCache, technicalsCache, avClient);
        final var snapshots = req.getSymbols().stream()
                .map(s -> {
                    try {
                        return fetcher.fetch(s, req.getExchange());
                    } catch (Exception e) {
                        log.warn("Failed to fetch metrics for {}: {}", s, e.getMessage());
                        return MetricSnapshot.empty(s, req.getExchange());
                    }
                })
                .collect(Collectors.toList());

        final var engine = new AnalysisEngine();
        final var scores = engine.analyze(req.getModelType(), snapshots, req.getParams());

        final var ranked = scores.stream()
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

    public AnalysisConfig saveConfig(final String userId, final String name,
                                     final AnalysisModelType modelType, final Object params) {
        try {
            final var paramsJson = objectMapper.writeValueAsString(params);
            final var config = AnalysisConfig.builder()
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

    public AnalysisConfig getConfig(final String configId) {
        return analysisConfigDao.findById(configId)
                .orElseThrow(() -> new NotFoundException("Analysis config not found: " + configId));
    }

    public List<AnalysisModelType> getAvailableModels() {
        return List.of(AnalysisModelType.values());
    }
}
