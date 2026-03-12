package com.stockmarket.analysis;

import com.stockmarket.model.enums.Exchange;
import java.util.Map;

public record StockScore(
        String symbol,
        Exchange exchange,
        double score,
        int rank,
        Map<String, Double> breakdown,
        Map<String, Double> rawMetrics
) {}
