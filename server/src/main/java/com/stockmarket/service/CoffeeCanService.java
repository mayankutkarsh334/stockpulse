package com.stockmarket.service;

import com.stockmarket.csv.CsvStockParser;
import com.stockmarket.model.dto.request.CoffeeCanScreenRequest;
import com.stockmarket.model.dto.response.CoffeeCanScreenResponse;
import com.stockmarket.model.dto.response.CoffeeCanScreenResponse.CoffeeCanStock;
import com.stockmarket.model.enums.Exchange;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class CoffeeCanService {

    // Hard filter thresholds
    private static final double ROCE_MIN           = 15.0;
    private static final double REVENUE_GROWTH_MIN = 10.0;
    private static final double MARKET_CAP_MIN_CR  = 500.0;

    // Soft flag thresholds
    private static final double DEBT_EQUITY_MAX    = 1.0;
    private static final double PLEDGED_MAX        = 10.0;

    private final CsvStockParser parser;

    @Inject
    public CoffeeCanService(final CsvStockParser parser) {
        this.parser = parser;
    }

    public CoffeeCanScreenResponse screen(final CoffeeCanScreenRequest req) {
        final var parsed = parser.parse(req.getCsvContent(), Exchange.NSE);

        final List<CoffeeCanStock> stocks = parsed.stream().map(p -> {
            final var snap = p.snapshot();
            final List<String> hardFails = new ArrayList<>();
            final List<String> softFlags = new ArrayList<>();

            // Hard filters
            if (isMissing(snap.roce()))
                hardFails.add("Missing: ROCE");
            else if (snap.roce() < ROCE_MIN)
                hardFails.add("ROCE below 15%");

            if (isMissing(snap.revenueGrowth()))
                hardFails.add("Missing: REVENUE_GROWTH");
            else if (snap.revenueGrowth() < REVENUE_GROWTH_MIN)
                hardFails.add("Revenue growth below 10%");

            if (snap.marketCap() != null && snap.marketCap() < MARKET_CAP_MIN_CR)
                hardFails.add("Market cap below \u20b9500 crore");

            // Soft flags
            if (snap.debtToEquity() != null && snap.debtToEquity() > DEBT_EQUITY_MAX)
                softFlags.add("D/E above 1");

            if (snap.pledgedPromoterHoldings() != null && snap.pledgedPromoterHoldings() > PLEDGED_MAX)
                softFlags.add("Pledged above 10%");

            final String tier = !hardFails.isEmpty() ? "FAIL"
                    : !softFlags.isEmpty() ? "TIER_2"
                    : "TIER_1";

            final List<String> allReasons = new ArrayList<>(hardFails);
            allReasons.addAll(softFlags);

            return CoffeeCanStock.builder()
                    .symbol(snap.symbol())
                    .companyName(p.companyName())
                    .roce(snap.roce())
                    .debtToEquity(snap.debtToEquity())
                    .revenueGrowth(snap.revenueGrowth())
                    .pledgedPromoterHoldings(snap.pledgedPromoterHoldings())
                    .pbRatio(snap.pbRatio())
                    .marketCap(snap.marketCap())
                    .tier(tier)
                    .failReasons(allReasons)
                    .build();
        }).collect(Collectors.toList());

        return CoffeeCanScreenResponse.builder()
                .stocks(stocks)
                .totalScanned(stocks.size())
                .totalTier1((int) stocks.stream().filter(s -> "TIER_1".equals(s.getTier())).count())
                .totalTier2((int) stocks.stream().filter(s -> "TIER_2".equals(s.getTier())).count())
                .build();
    }

    private boolean isMissing(final Double value) {
        return value == null || Double.isNaN(value);
    }
}
