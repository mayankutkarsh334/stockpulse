package com.stockmarket.service;

import com.stockmarket.client.AlphaVantageClient;
import com.stockmarket.dao.aerospike.FundamentalsCache;
import com.stockmarket.dao.aerospike.TechnicalsCache;
import com.stockmarket.kafka.producer.AnalysisJobProducer;
import com.stockmarket.model.cache.StockFundamentals;
import com.stockmarket.model.cache.StockTechnicals;
import com.stockmarket.model.dto.request.ScreenerScanRequest;
import com.stockmarket.model.dto.response.ScreenerResultResponse;
import com.stockmarket.model.enums.ScreenerIndicator;
import com.stockmarket.screener.ScreenerEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class ScreenerService {

    private final FundamentalsCache fundamentalsCache;
    private final TechnicalsCache technicalsCache;
    private final AlphaVantageClient avClient;
    private final AnalysisJobProducer analysisJobProducer;
    private final ScreenerEngine screenerEngine = new ScreenerEngine();

    public ScreenerResultResponse scan(ScreenerScanRequest req) {
        List<ScreenerResultResponse.ScreenerMatch> matches = new ArrayList<>();
        for (String symbol : req.getSymbols()) {
            try {
                StockFundamentals fundamentals = fundamentalsCache.get(symbol, req.getExchange())
                        .orElseGet(() -> {
                            try {
                                StockFundamentals f = avClient.fetchFundamentals(symbol, req.getExchange());
                                fundamentalsCache.put(f);
                                return f;
                            } catch (Exception e) {
                                log.warn("Failed to fetch fundamentals for {}: {}", symbol, e.getMessage());
                                return StockFundamentals.builder().symbol(symbol).exchange(req.getExchange()).build();
                            }
                        });

                StockTechnicals technicals = technicalsCache.get(symbol, req.getExchange())
                        .orElseGet(() -> {
                            try {
                                StockTechnicals t = avClient.fetchTechnicals(symbol, req.getExchange());
                                technicalsCache.put(t);
                                return t;
                            } catch (Exception e) {
                                log.warn("Failed to fetch technicals for {}: {}", symbol, e.getMessage());
                                return StockTechnicals.builder().symbol(symbol).exchange(req.getExchange()).build();
                            }
                        });

                var result = screenerEngine.evaluate(symbol, req.getExchange(), fundamentals, technicals, req.getFilters());
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
