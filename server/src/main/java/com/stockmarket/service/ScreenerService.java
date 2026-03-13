package com.stockmarket.service;

import com.stockmarket.client.AlphaVantageClient;
import com.stockmarket.dao.aerospike.FundamentalsCache;
import com.stockmarket.dao.aerospike.TechnicalsCache;
import com.stockmarket.kafka.producer.AnalysisJobProducer;
import com.stockmarket.model.dto.request.ScreenerScanRequest;
import com.stockmarket.model.dto.response.ScreenerResultResponse;
import com.stockmarket.screener.ScreenerEngine;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class ScreenerService {

    private final FundamentalsCache fundamentalsCache;
    private final TechnicalsCache technicalsCache;
    private final AlphaVantageClient avClient;
    private final AnalysisJobProducer analysisJobProducer;
    private final ScreenerEngine screenerEngine = new ScreenerEngine();

    @Inject
    public ScreenerService(final FundamentalsCache fundamentalsCache,
                           final TechnicalsCache technicalsCache,
                           final AlphaVantageClient avClient,
                           final AnalysisJobProducer analysisJobProducer) {
        this.fundamentalsCache = fundamentalsCache;
        this.technicalsCache = technicalsCache;
        this.avClient = avClient;
        this.analysisJobProducer = analysisJobProducer;
    }

    public ScreenerResultResponse scan(final ScreenerScanRequest req) {
        final var matches = new ArrayList<ScreenerResultResponse.ScreenerMatch>();
        for (final String symbol : req.getSymbols()) {
            try {
                final var fundamentals = fundamentalsCache.get(symbol, req.getExchange())
                        .orElseGet(() -> {
                            try {
                                final var f = avClient.fetchFundamentals(symbol, req.getExchange());
                                fundamentalsCache.put(f);
                                return f;
                            } catch (Exception e) {
                                log.warn("Failed to fetch fundamentals for {}: {}", symbol, e.getMessage());
                                return com.stockmarket.model.cache.StockFundamentals.builder()
                                        .symbol(symbol).exchange(req.getExchange()).build();
                            }
                        });

                final var technicals = technicalsCache.get(symbol, req.getExchange())
                        .orElseGet(() -> {
                            try {
                                final var t = avClient.fetchTechnicals(symbol, req.getExchange());
                                technicalsCache.put(t);
                                return t;
                            } catch (Exception e) {
                                log.warn("Failed to fetch technicals for {}: {}", symbol, e.getMessage());
                                return com.stockmarket.model.cache.StockTechnicals.builder()
                                        .symbol(symbol).exchange(req.getExchange()).build();
                            }
                        });

                final var result = screenerEngine.evaluate(symbol, req.getExchange(), fundamentals, technicals, req.getFilters());
                if (result != null) matches.add(result);
            } catch (Exception e) {
                log.error("Error evaluating screener for {}: {}", symbol, e.getMessage());
            }
        }

        return ScreenerResultResponse.builder()
                .matches(matches)
                .totalScanned(req.getSymbols().size())
                .totalMatched(matches.size())
                .build();
    }

    public List<String> getAvailablePresets() {
        return List.of("OVERSOLD_RSI", "HIGH_PE_GROWTH", "LARGECAP_VALUE");
    }
}
