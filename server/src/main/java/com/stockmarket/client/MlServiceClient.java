package com.stockmarket.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmarket.config.MlServiceConfig;
import com.stockmarket.model.dto.response.SimulationResult;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Singleton
public class MlServiceClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final MlServiceConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Inject
    public MlServiceClient(final MlServiceConfig config, final ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(120)) // Monte Carlo can take time
                .build();
    }

    /**
     * Calls POST /forecast on the Python ml-service and returns the simulation result.
     *
     * @throws IOException if the ml-service is unreachable or returns an error
     */
    public SimulationResult forecast(
            final String symbol,
            final String exchange,
            final double entryPrice,
            final double stopLoss,
            final int numPaths,
            final int horizonDays,
            final double targetPct) throws IOException {

        final var payload = new HashMap<String, Object>();
        payload.put("symbol", symbol);
        payload.put("exchange", exchange);
        payload.put("entry_price", entryPrice);
        payload.put("stop_loss", stopLoss);
        payload.put("num_paths", numPaths);
        payload.put("horizon_days", horizonDays);
        payload.put("target_pct", targetPct);

        final String json = objectMapper.writeValueAsString(payload);
        final RequestBody body = RequestBody.create(json, JSON);
        final Request request = new Request.Builder()
                .url(config.getBaseUrl() + "/forecast")
                .post(body)
                .build();

        log.debug("Calling ml-service: POST {}/forecast symbol={} exchange={}", config.getBaseUrl(), symbol, exchange);

        try (final Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                final String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("ml-service returned HTTP " + response.code() + ": " + errorBody);
            }
            final String responseJson = response.body().string();
            return objectMapper.readValue(responseJson, SimulationResult.class);
        }
    }

    /**
     * Calls POST /forecast on the Python ml-service with optional extra indicators.
     *
     * @throws IOException if the ml-service is unreachable or returns an error
     */
    public SimulationResult forecastWithIndicators(
            final String symbol,
            final String exchange,
            final double entryPrice,
            final double stopLoss,
            final double targetPct,
            final List<Map<String, Object>> extraIndicators) throws IOException {

        final var payload = new HashMap<String, Object>();
        payload.put("symbol", symbol);
        payload.put("exchange", exchange);
        payload.put("entry_price", entryPrice);
        payload.put("stop_loss", stopLoss);
        payload.put("num_paths", 10_000);
        payload.put("horizon_days", 504);
        payload.put("target_pct", targetPct);
        if (extraIndicators != null && !extraIndicators.isEmpty()) {
            payload.put("extra_indicators", extraIndicators);
        }

        final String json = objectMapper.writeValueAsString(payload);
        final RequestBody body = RequestBody.create(json, JSON);
        final Request request = new Request.Builder()
                .url(config.getBaseUrl() + "/forecast")
                .post(body)
                .build();

        log.debug("Calling ml-service with indicators: POST {}/forecast symbol={} exchange={} extraIndicators={}",
                config.getBaseUrl(), symbol, exchange,
                extraIndicators != null ? extraIndicators.size() : 0);

        try (final Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                final String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("ml-service returned HTTP " + response.code() + ": " + errorBody);
            }
            return objectMapper.readValue(response.body().string(), SimulationResult.class);
        }
    }

    /**
     * Calls GET /price/{symbol}?exchange={exchange} on the Python ml-service.
     *
     * @return the latest closing price, or empty if unavailable
     */
    public Optional<Double> getPrice(final String symbol, final String exchange) throws IOException {
        final String url = config.getBaseUrl() + "/price/" + symbol + "?exchange=" + exchange;
        final Request request = new Request.Builder().url(url).get().build();

        log.debug("Calling ml-service: GET {} symbol={} exchange={}", url, symbol, exchange);

        try (final Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) return Optional.empty();
            @SuppressWarnings("unchecked")
            final Map<String, Object> body = objectMapper.readValue(response.body().string(), Map.class);
            final Object price = body.get("price");
            if (price instanceof Number) return Optional.of(((Number) price).doubleValue());
            return Optional.empty();
        }
    }
}
