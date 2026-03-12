package com.stockmarket.screener;

import com.stockmarket.model.cache.StockFundamentals;
import com.stockmarket.model.cache.StockTechnicals;
import com.stockmarket.model.dto.request.ScreenerScanRequest;
import com.stockmarket.model.dto.response.ScreenerResultResponse;
import com.stockmarket.model.enums.Exchange;
import com.stockmarket.model.enums.ScreenerIndicator;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Slf4j
public class ScreenerEngine {

    public ScreenerResultResponse.ScreenerMatch evaluate(String symbol, Exchange exchange,
                                                          StockFundamentals fundamentals,
                                                          StockTechnicals technicals,
                                                          List<ScreenerScanRequest.FilterCriteria> filters) {
        if (filters == null || filters.isEmpty()) {
            return buildMatch(symbol, exchange, fundamentals, technicals, List.of());
        }

        List<String> passed = new ArrayList<>();
        for (ScreenerScanRequest.FilterCriteria filter : filters) {
            Double metricValue = resolveMetric(filter.getIndicator(), fundamentals, technicals);
            if (metricValue == null) continue;
            if (evaluate(metricValue, filter.getOperator(), filter.getValue())) {
                passed.add(filter.getIndicator().name() + " " + filter.getOperator() + " " + filter.getValue());
            } else {
                return null; // All filters must pass (AND logic)
            }
        }
        return buildMatch(symbol, exchange, fundamentals, technicals, passed);
    }

    private ScreenerResultResponse.ScreenerMatch buildMatch(String symbol, Exchange exchange,
                                                             StockFundamentals f, StockTechnicals t,
                                                             List<String> passed) {
        Map<ScreenerIndicator, Double> metrics = new LinkedHashMap<>();
        putIfNotNull(metrics, ScreenerIndicator.PE, f.getPe());
        putIfNotNull(metrics, ScreenerIndicator.EPS, f.getEps());
        putIfNotNull(metrics, ScreenerIndicator.RSI_14, t.getRsi14());
        putIfNotNull(metrics, ScreenerIndicator.MACD_VALUE, t.getMacdValue());
        putIfNotNull(metrics, ScreenerIndicator.SMA_20, t.getSma20());
        putIfNotNull(metrics, ScreenerIndicator.SMA_50, t.getSma50());
        putIfNotNull(metrics, ScreenerIndicator.MARKET_CAP, f.getMarketCap() != null ? f.getMarketCap().doubleValue() : null);
        putIfNotNull(metrics, ScreenerIndicator.CURRENT_PRICE, t.getCurrentPrice());
        return ScreenerResultResponse.ScreenerMatch.builder()
                .symbol(symbol).exchange(exchange).metricValues(metrics).passedFilters(passed).build();
    }

    private void putIfNotNull(Map<ScreenerIndicator, Double> map, ScreenerIndicator key, Double value) {
        if (value != null) map.put(key, value);
    }

    private boolean evaluate(double metricValue, String operator, double threshold) {
        return switch (operator.toUpperCase()) {
            case "GT" -> metricValue > threshold;
            case "GTE" -> metricValue >= threshold;
            case "LT" -> metricValue < threshold;
            case "LTE" -> metricValue <= threshold;
            case "EQ" -> Math.abs(metricValue - threshold) < 0.0001;
            default -> {
                log.warn("Unknown operator: {}", operator);
                yield false;
            }
        };
    }

    private Double resolveMetric(ScreenerIndicator indicator, StockFundamentals f, StockTechnicals t) {
        return switch (indicator) {
            case PE -> f.getPe();
            case EPS -> f.getEps();
            case EPS_GROWTH -> f.getEpsGrowth();
            case REVENUE_GROWTH -> f.getRevenueGrowth();
            case MARKET_CAP -> f.getMarketCap() != null ? f.getMarketCap().doubleValue() : null;
            case DIVIDEND_YIELD -> f.getDividendYield();
            case DEBT_TO_EQUITY -> f.getDebtToEquity();
            case CURRENT_RATIO -> f.getCurrentRatio();
            case ROA -> f.getRoa();
            case GROSS_MARGIN -> f.getGrossMargin();
            case ASSET_TURNOVER -> f.getAssetTurnover();
            case RETAINED_EARNINGS -> f.getRetainedEarnings();
            case EBIT -> f.getEbit();
            case WORKING_CAPITAL -> f.getWorkingCapital();
            case TOTAL_ASSETS -> f.getTotalAssets();
            case TOTAL_LIABILITIES -> f.getTotalLiabilities();
            case OPERATING_CASH_FLOW -> f.getOperatingCashFlow();
            case SHARES_OUTSTANDING -> f.getSharesOutstanding() != null ? f.getSharesOutstanding().doubleValue() : null;
            case RSI_14 -> t.getRsi14();
            case MACD_VALUE -> t.getMacdValue();
            case MACD_SIGNAL -> t.getMacdSignal();
            case MACD_HISTOGRAM -> t.getMacdHistogram();
            case SMA_20 -> t.getSma20();
            case SMA_50 -> t.getSma50();
            case SMA_200 -> t.getSma200();
            case HIGH_52W -> f.getHigh52w();
            case LOW_52W -> f.getLow52w();
            case CURRENT_PRICE -> t.getCurrentPrice();
            case VOLUME -> t.getVolume();
            case AVG_VOLUME -> t.getAvgVolume();
        };
    }
}
