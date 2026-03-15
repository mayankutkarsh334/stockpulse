package com.stockmarket.service;

import com.stockmarket.client.MlServiceClient;
import com.stockmarket.model.dto.response.SimulationResult;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Singleton
public class ForecastService {

    private static final int DEFAULT_NUM_PATHS = 10_000;
    private static final int DEFAULT_HORIZON_DAYS = 504;

    private final MlServiceClient mlServiceClient;

    @Inject
    public ForecastService(final MlServiceClient mlServiceClient) {
        this.mlServiceClient = mlServiceClient;
    }

    /**
     * Run a Monte Carlo + ARIMA + GARCH forecast for the given symbol.
     *
     * @return an Optional containing the result, or empty if ml-service is unavailable
     */
    public Optional<SimulationResult> forecast(
            final String symbol,
            final String exchange,
            final double entryPrice,
            final double stopLoss,
            final double targetPct) {

        try {
            final SimulationResult result = mlServiceClient.forecast(
                    symbol, exchange, entryPrice, stopLoss,
                    DEFAULT_NUM_PATHS, DEFAULT_HORIZON_DAYS, targetPct);
            return Optional.of(result);
        } catch (IOException e) {
            log.warn("ml-service unavailable for symbol={}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Run a forecast with optional extra CSV indicators for the given symbol.
     *
     * @return an Optional containing the result, or empty if ml-service is unavailable
     */
    public Optional<SimulationResult> forecastWithIndicators(
            final String symbol,
            final String exchange,
            final double entryPrice,
            final double stopLoss,
            final double targetPct,
            final List<Map<String, Object>> extraIndicators) {

        try {
            final SimulationResult result = mlServiceClient.forecastWithIndicators(
                    symbol, exchange, entryPrice, stopLoss, targetPct, extraIndicators);
            return Optional.of(result);
        } catch (IOException e) {
            log.warn("ml-service unavailable for symbol={}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Fetch the latest closing price for a symbol from ml-service.
     *
     * @return the price, or empty if unavailable
     */
    public Optional<Double> getPrice(final String symbol, final String exchange) {
        try {
            return mlServiceClient.getPrice(symbol, exchange);
        } catch (IOException e) {
            log.warn("ml-service price unavailable for symbol={}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }
}
