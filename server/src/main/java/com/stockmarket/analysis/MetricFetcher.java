package com.stockmarket.analysis;

import com.stockmarket.client.AlphaVantageClient;
import com.stockmarket.dao.aerospike.FundamentalsCache;
import com.stockmarket.dao.aerospike.TechnicalsCache;
import com.stockmarket.model.cache.StockFundamentals;
import com.stockmarket.model.cache.StockTechnicals;
import com.stockmarket.model.enums.Exchange;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Singleton
public class MetricFetcher {

    private final FundamentalsCache fundamentalsCache;
    private final TechnicalsCache technicalsCache;
    private final AlphaVantageClient avClient;
    private final ExecutorService executor;

    @Inject
    public MetricFetcher(final FundamentalsCache fundamentalsCache,
                         final TechnicalsCache technicalsCache,
                         final AlphaVantageClient avClient,
                         final ExecutorService executor) {
        this.fundamentalsCache = fundamentalsCache;
        this.technicalsCache = technicalsCache;
        this.avClient = avClient;
        this.executor = executor;
    }

    public MetricSnapshot fetch(final String symbol, final Exchange exchange) throws Exception {
        return fetch(symbol, exchange, Set.of());
    }

    public MetricSnapshot fetch(final String symbol, final Exchange exchange,
                                 final Set<String> requiredMetrics) throws Exception {
        final var fundamentalsFuture = CompletableFuture.supplyAsync(() ->
                fundamentalsCache.get(symbol, exchange).orElseGet(() -> {
                    try {
                        final var f = avClient.fetchFundamentals(symbol, exchange);
                        fundamentalsCache.put(f);
                        return f;
                    } catch (Exception e) {
                        log.warn("Failed to fetch fundamentals for {}: {}", symbol, e.getMessage());
                        return StockFundamentals.builder().symbol(symbol).exchange(exchange).build();
                    }
                }), executor);

        final var technicalsFuture = CompletableFuture.supplyAsync(() ->
                technicalsCache.get(symbol, exchange).orElseGet(() -> {
                    try {
                        final var t = avClient.fetchTechnicals(symbol, exchange, requiredMetrics);
                        technicalsCache.put(t);
                        return t;
                    } catch (Exception e) {
                        log.warn("Failed to fetch technicals for {}: {}", symbol, e.getMessage());
                        return StockTechnicals.builder().symbol(symbol).exchange(exchange).build();
                    }
                }), executor);

        CompletableFuture.allOf(fundamentalsFuture, technicalsFuture).join();
        return buildSnapshot(fundamentalsFuture.join(), technicalsFuture.join());
    }

    private MetricSnapshot buildSnapshot(final StockFundamentals f, final StockTechnicals t) {
        return new MetricSnapshot(
                f.getSymbol(), f.getExchange(),
                f.getPe(), f.getEps(), f.getEpsGrowth(), f.getRevenueGrowth(),
                f.getMarketCap() != null ? f.getMarketCap().doubleValue() : null,
                f.getDividendYield(), f.getDebtToEquity(), f.getCurrentRatio(),
                f.getRoa(), f.getGrossMargin(), f.getAssetTurnover(),
                f.getRetainedEarnings(), f.getEbit(), f.getWorkingCapital(),
                f.getTotalAssets(), f.getTotalLiabilities(), f.getOperatingCashFlow(),
                f.getSharesOutstanding(), f.getSector(), f.getIndustry(),
                t.getRsi14(), t.getMacdValue(), t.getMacdSignal(), t.getMacdHistogram(),
                t.getSma20(), t.getSma50(), t.getSma200(),
                f.getHigh52w(), f.getLow52w(), t.getCurrentPrice(),
                t.getVolume(), t.getAvgVolume(),
                // CSV-specific fields not populated from Alpha Vantage
                null, null, null, null, null, null, null, null, null, null, null, null
        );
    }
}
