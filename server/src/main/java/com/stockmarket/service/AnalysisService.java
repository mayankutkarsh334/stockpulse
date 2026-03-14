package com.stockmarket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmarket.analysis.AnalysisEngine;
import com.stockmarket.analysis.MetricFetcher;
import com.stockmarket.analysis.MetricSnapshot;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class AnalysisService {

    private final MetricFetcher fetcher;
    private final AnalysisEngine engine;
    private final ExecutorService executor;
    private final AnalysisConfigDao analysisConfigDao;
    private final AnalysisJobProducer analysisJobProducer;
    private final ObjectMapper objectMapper;

    @Inject
    public AnalysisService(final MetricFetcher fetcher,
                           final AnalysisEngine engine,
                           final ExecutorService executor,
                           final AnalysisConfigDao analysisConfigDao,
                           final AnalysisJobProducer analysisJobProducer,
                           final ObjectMapper objectMapper) {
        this.fetcher = fetcher;
        this.engine = engine;
        this.executor = executor;
        this.analysisConfigDao = analysisConfigDao;
        this.analysisJobProducer = analysisJobProducer;
        this.objectMapper = objectMapper;
    }

    public AnalysisResultResponse runAnalysis(final AnalysisRequest req) {
        final Set<String> required = engine.requiredMetrics(req.getModelType(), req.getParams());

        final var futures = req.getSymbols().stream()
                .map(symbol -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return fetcher.fetch(symbol, req.getExchange(), required);
                    } catch (Exception e) {
                        log.warn("Failed to fetch metrics for {}: {}", symbol, e.getMessage());
                        return MetricSnapshot.empty(symbol, req.getExchange());
                    }
                }, executor))
                .collect(Collectors.toList());

        final var snapshots = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

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
