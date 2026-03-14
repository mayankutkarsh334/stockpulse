package com.stockmarket.analysis;

import com.stockmarket.model.enums.Exchange;

public record MetricSnapshot(
        String symbol,
        Exchange exchange,
        // Fundamentals
        Double pe,
        Double eps,
        Double epsGrowth,
        Double revenueGrowth,
        Double marketCap,
        Double dividendYield,
        Double debtToEquity,
        Double currentRatio,
        Double roa,
        Double grossMargin,
        Double assetTurnover,
        Double retainedEarnings,
        Double ebit,
        Double workingCapital,
        Double totalAssets,
        Double totalLiabilities,
        Double operatingCashFlow,
        Long sharesOutstanding,
        String sector,
        String industry,
        // Technicals
        Double rsi14,
        Double macdValue,
        Double macdSignal,
        Double macdHistogram,
        Double sma20,
        Double sma50,
        Double sma200,
        Double high52w,
        Double low52w,
        Double currentPrice,
        Double volume,
        Double avgVolume,
        // CSV / India screener specific
        String companyName,
        Double roe,
        Double roce,
        Double pbRatio,
        Double forwardPe,
        Double pledgedPromoterHoldings,
        Double oneMonthReturn,
        Double sixMonthReturn,
        Double oneYearReturn,
        Double returnVsNifty,
        Double percentAwayFrom52wHigh,
        Double closePrice
) {
    public static MetricSnapshot empty(String symbol, Exchange exchange) {
        return new MetricSnapshot(symbol, exchange,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public Double getByName(String name) {
        return switch (name.toUpperCase()) {
            case "PE" -> pe;
            case "EPS" -> eps;
            case "EPS_GROWTH" -> epsGrowth;
            case "REVENUE_GROWTH" -> revenueGrowth;
            case "MARKET_CAP" -> marketCap;
            case "DIVIDEND_YIELD" -> dividendYield;
            case "DEBT_TO_EQUITY" -> debtToEquity;
            case "CURRENT_RATIO" -> currentRatio;
            case "ROA" -> roa;
            case "GROSS_MARGIN" -> grossMargin;
            case "ASSET_TURNOVER" -> assetTurnover;
            case "RETAINED_EARNINGS" -> retainedEarnings;
            case "EBIT" -> ebit;
            case "WORKING_CAPITAL" -> workingCapital;
            case "TOTAL_ASSETS" -> totalAssets;
            case "TOTAL_LIABILITIES" -> totalLiabilities;
            case "OPERATING_CASH_FLOW" -> operatingCashFlow;
            case "SHARES_OUTSTANDING" -> sharesOutstanding != null ? sharesOutstanding.doubleValue() : null;
            case "RSI_14" -> rsi14;
            case "MACD_VALUE" -> macdValue;
            case "MACD_SIGNAL" -> macdSignal;
            case "MACD_HISTOGRAM" -> macdHistogram;
            case "SMA_20" -> sma20;
            case "SMA_50" -> sma50;
            case "SMA_200" -> sma200;
            case "HIGH_52W" -> high52w;
            case "LOW_52W" -> low52w;
            case "CURRENT_PRICE" -> currentPrice;
            case "VOLUME" -> volume;
            case "AVG_VOLUME" -> avgVolume;
            // CSV / India screener metrics
            case "ROE" -> roe;
            case "ROCE" -> roce;
            case "PB_RATIO" -> pbRatio;
            case "FORWARD_PE" -> forwardPe;
            case "PLEDGED_PROMOTER_HOLDINGS" -> pledgedPromoterHoldings;
            case "ONE_MONTH_RETURN" -> oneMonthReturn;
            case "SIX_MONTH_RETURN" -> sixMonthReturn;
            case "ONE_YEAR_RETURN" -> oneYearReturn;
            case "RETURN_VS_NIFTY" -> returnVsNifty;
            case "PCT_AWAY_52W_HIGH" -> percentAwayFrom52wHigh;
            case "CLOSE_PRICE" -> closePrice;
            default -> null;
        };
    }
}
