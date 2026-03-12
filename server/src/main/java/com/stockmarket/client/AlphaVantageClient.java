package com.stockmarket.client;

import com.google.common.util.concurrent.RateLimiter;
import com.stockmarket.config.AlphaVantageConfig;
import com.stockmarket.model.cache.StockFundamentals;
import com.stockmarket.model.cache.StockQuote;
import com.stockmarket.model.cache.StockTechnicals;
import com.stockmarket.model.enums.Exchange;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class AlphaVantageClient {

    private final AlphaVantageConfig config;
    private final OkHttpClient httpClient;
    private final RateLimiter rateLimiter;
    private final CircuitBreaker circuitBreaker;
    private final AtomicInteger dailyCallCount = new AtomicInteger(0);

    public AlphaVantageClient(AlphaVantageConfig config) {
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .build();
        double ratePerSecond = (double) config.getRateLimitPerMinute() / 60.0;
        this.rateLimiter = RateLimiter.create(ratePerSecond);
        this.circuitBreaker = CircuitBreaker.of("alphavantage",
                CircuitBreakerConfig.custom()
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofMinutes(5))
                        .slidingWindowSize(10)
                        .build());
    }

    private String avSymbol(String symbol, Exchange exchange) {
        return symbol.toUpperCase() + exchange.toAlphaVantageSuffix();
    }

    private String fetch(String url) throws IOException {
        if (dailyCallCount.get() >= config.getDailyCallLimit()) {
            throw new RateLimitException("Daily Alpha Vantage call limit reached: " + config.getDailyCallLimit());
        }
        rateLimiter.acquire();
        try {
            return circuitBreaker.executeCheckedSupplier(() -> {
                Request request = new Request.Builder().url(url).build();
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Alpha Vantage returned HTTP " + response.code());
                    }
                    dailyCallCount.incrementAndGet();
                    return response.body().string();
                }
            });
        } catch (IOException e) {
            throw e;
        } catch (Throwable t) {
            throw new IOException("Circuit breaker error: " + t.getMessage(), t);
        }
    }

    public String fetchQuoteRaw(String symbol, Exchange exchange) throws IOException {
        String avSym = avSymbol(symbol, exchange);
        String url = config.getBaseUrl() + "?function=GLOBAL_QUOTE&symbol=" + avSym + "&apikey=" + config.getApiKey();
        log.debug("Fetching quote for {}", avSym);
        return fetch(url);
    }

    public String fetchOverviewRaw(String symbol, Exchange exchange) throws IOException {
        String avSym = avSymbol(symbol, exchange);
        String url = config.getBaseUrl() + "?function=OVERVIEW&symbol=" + avSym + "&apikey=" + config.getApiKey();
        log.debug("Fetching overview for {}", avSym);
        return fetch(url);
    }

    public String fetchRsiRaw(String symbol, Exchange exchange) throws IOException {
        String avSym = avSymbol(symbol, exchange);
        String url = config.getBaseUrl() + "?function=RSI&symbol=" + avSym +
                "&interval=daily&time_period=14&series_type=close&apikey=" + config.getApiKey();
        return fetch(url);
    }

    public String fetchMacdRaw(String symbol, Exchange exchange) throws IOException {
        String avSym = avSymbol(symbol, exchange);
        String url = config.getBaseUrl() + "?function=MACD&symbol=" + avSym +
                "&interval=daily&series_type=close&apikey=" + config.getApiKey();
        return fetch(url);
    }

    public String fetchSmaRaw(String symbol, Exchange exchange, int period) throws IOException {
        String avSym = avSymbol(symbol, exchange);
        String url = config.getBaseUrl() + "?function=SMA&symbol=" + avSym +
                "&interval=daily&time_period=" + period + "&series_type=close&apikey=" + config.getApiKey();
        return fetch(url);
    }

    public String fetchDailyTimeSeriesRaw(String symbol, Exchange exchange) throws IOException {
        String avSym = avSymbol(symbol, exchange);
        String url = config.getBaseUrl() + "?function=TIME_SERIES_DAILY&symbol=" + avSym +
                "&outputsize=compact&apikey=" + config.getApiKey();
        return fetch(url);
    }

    public StockQuote fetchQuote(String symbol, Exchange exchange) throws IOException {
        String json = fetchQuoteRaw(symbol, exchange);
        return AlphaVantageResponseParser.parseQuote(json, symbol, exchange);
    }

    public StockFundamentals fetchFundamentals(String symbol, Exchange exchange) throws IOException {
        String json = fetchOverviewRaw(symbol, exchange);
        return AlphaVantageResponseParser.parseFundamentals(json, symbol, exchange);
    }

    public StockTechnicals fetchTechnicals(String symbol, Exchange exchange) throws IOException {
        String rsiJson = fetchRsiRaw(symbol, exchange);
        String macdJson = fetchMacdRaw(symbol, exchange);
        String sma20Json = fetchSmaRaw(symbol, exchange, 20);
        String sma50Json = fetchSmaRaw(symbol, exchange, 50);
        String sma200Json = fetchSmaRaw(symbol, exchange, 200);
        return AlphaVantageResponseParser.parseTechnicals(rsiJson, macdJson, sma20Json, sma50Json, sma200Json, symbol, exchange);
    }

    public boolean isHealthy() {
        return circuitBreaker.getState() != CircuitBreaker.State.OPEN;
    }

    public void resetDailyCount() {
        dailyCallCount.set(0);
    }

    public int getDailyCallCount() {
        return dailyCallCount.get();
    }

    public static class RateLimitException extends IOException {
        public RateLimitException(String message) {
            super(message);
        }
    }
}
