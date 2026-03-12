package com.stockmarket.analysis;

import com.stockmarket.client.AlphaVantageClient;
import com.stockmarket.dao.aerospike.FundamentalsCache;
import com.stockmarket.dao.aerospike.TechnicalsCache;
import com.stockmarket.model.cache.StockFundamentals;
import com.stockmarket.model.cache.StockTechnicals;
import com.stockmarket.model.enums.Exchange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MetricFetcher {

    private final FundamentalsCache fundamentalsCache;
    private final TechnicalsCache technicalsCache;
    private final AlphaVantageClient avClient;

    public MetricSnapshot fetch(String symbol, Exchange exchange) throws Exception {
        StockFundamentals f = fundamentalsCache.get(symbol, exchange).orElseGet(() -> {
            try {
                StockFundamentals fetched = avClient.fetchFundamentals(symbol, exchange);
                fundamentalsCache.put(fetched);
                return fetched;
            } catch (Exception e) {
                log.warn("Failed to fetch fundamentals for {}: {}", symbol, e.getMessage());
                return StockFundamentals.builder().symbol(symbol).exchange(exchange).build();
            }
        });

        StockTechnicals t = technicalsCache.get(symbol, exchange).orElseGet(() -> {
            try {
                StockTechnicals fetched = avClient.fetchTechnicals(symbol, exchange);
                technicalsCache.put(fetched);
                return fetched;
            } catch (Exception e) {
                log.warn("Failed to fetch technicals for {}: {}", symbol, e.getMessage());
                return StockTechnicals.builder().symbol(symbol).exchange(exchange).build();
            }
        });

        return new MetricSnapshot(
                symbol, exchange,
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
                t.getVolume(), t.getAvgVolume()
        );
    }
}
