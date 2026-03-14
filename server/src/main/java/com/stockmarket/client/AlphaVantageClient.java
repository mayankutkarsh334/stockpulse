package com.stockmarket.client;

import com.google.common.util.concurrent.RateLimiter;
import com.stockmarket.config.AlphaVantageConfig;
import com.stockmarket.model.cache.StockFundamentals;
import com.stockmarket.model.cache.StockQuote;
import com.stockmarket.model.cache.StockTechnicals;
import com.stockmarket.model.enums.Exchange;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Singleton
public class AlphaVantageClient {

    private final AlphaVantageConfig config;
    private final OkHttpClient httpClient;
    private final RateLimiter rateLimiter;
    private final CircuitBreaker circuitBreaker;
    private final AtomicInteger dailyCallCount = new AtomicInteger(0);
    private final ExecutorService executor;

    @Inject
    public AlphaVantageClient(final AlphaVantageConfig config, final ExecutorService executor) {
        this.executor = executor;
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .build();
        final var ratePerSecond = (double) config.getRateLimitPerMinute() / 60.0;
        this.rateLimiter = RateLimiter.create(ratePerSecond);
        this.circuitBreaker = CircuitBreaker.of("alphavantage",
                CircuitBreakerConfig.custom()
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofMinutes(5))
                        .slidingWindowSize(10)
                        .build());
    }

    private String avSymbol(final String symbol, final Exchange exchange) {
        return symbol.toUpperCase() + exchange.toAlphaVantageSuffix();
    }

    private String fetch(final String url) throws IOException {
        if (dailyCallCount.get() >= config.getDailyCallLimit()) {
            throw new RateLimitException("Daily Alpha Vantage call limit reached: " + config.getDailyCallLimit());
        }
        rateLimiter.acquire();
        try {
            return circuitBreaker.executeCheckedSupplier(() -> {
                final var request = new Request.Builder().url(url).build();
                try (final Response response = httpClient.newCall(request).execute()) {
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

    public String fetchQuoteRaw(final String symbol, final Exchange exchange) throws IOException {
        final var avSym = avSymbol(symbol, exchange);
        final var url = config.getBaseUrl() + "?function=GLOBAL_QUOTE&symbol=" + avSym + "&apikey=" + config.getApiKey();
        log.debug("Fetching quote for {}", avSym);
        return fetch(url);
    }

    public String fetchOverviewRaw(final String symbol, final Exchange exchange) throws IOException {
        final var avSym = avSymbol(symbol, exchange);
        final var url = config.getBaseUrl() + "?function=OVERVIEW&symbol=" + avSym + "&apikey=" + config.getApiKey();
        log.debug("Fetching overview for {}", avSym);
        return fetch(url);
    }

    public String fetchRsiRaw(final String symbol, final Exchange exchange) throws IOException {
        final var avSym = avSymbol(symbol, exchange);
        final var url = config.getBaseUrl() + "?function=RSI&symbol=" + avSym +
                "&interval=daily&time_period=14&series_type=close&apikey=" + config.getApiKey();
        return fetch(url);
    }

    public String fetchMacdRaw(final String symbol, final Exchange exchange) throws IOException {
        final var avSym = avSymbol(symbol, exchange);
        final var url = config.getBaseUrl() + "?function=MACD&symbol=" + avSym +
                "&interval=daily&series_type=close&apikey=" + config.getApiKey();
        return fetch(url);
    }

    public String fetchSmaRaw(final String symbol, final Exchange exchange, final int period) throws IOException {
        final var avSym = avSymbol(symbol, exchange);
        final var url = config.getBaseUrl() + "?function=SMA&symbol=" + avSym +
                "&interval=daily&time_period=" + period + "&series_type=close&apikey=" + config.getApiKey();
        return fetch(url);
    }

    public String fetchDailyTimeSeriesRaw(final String symbol, final Exchange exchange) throws IOException {
        final var avSym = avSymbol(symbol, exchange);
        final var url = config.getBaseUrl() + "?function=TIME_SERIES_DAILY&symbol=" + avSym +
                "&outputsize=compact&apikey=" + config.getApiKey();
        return fetch(url);
    }

    public StockQuote fetchQuote(final String symbol, final Exchange exchange) throws IOException {
        final var json = fetchQuoteRaw(symbol, exchange);
        return AlphaVantageResponseParser.parseQuote(json, symbol, exchange);
    }

    public StockFundamentals fetchFundamentals(final String symbol, final Exchange exchange) throws IOException {
        final var json = fetchOverviewRaw(symbol, exchange);
        return AlphaVantageResponseParser.parseFundamentals(json, symbol, exchange);
    }

    public StockTechnicals fetchTechnicals(final String symbol, final Exchange exchange) throws IOException {
        return fetchTechnicals(symbol, exchange, Set.of());
    }

    public StockTechnicals fetchTechnicals(final String symbol, final Exchange exchange,
                                            final Set<String> required) throws IOException {
        final boolean all  = required.isEmpty();
        final boolean rsi  = all || required.contains("RSI_14");
        final boolean macd = all || required.stream().anyMatch(m -> m.startsWith("MACD"));
        final boolean s20  = all || required.contains("SMA_20");
        final boolean s50  = all || required.contains("SMA_50");
        final boolean s200 = all || required.contains("SMA_200");

        try {
            final var rsiFuture  = rsi  ? supplyFetch(() -> fetchRsiRaw(symbol, exchange))       : done(null);
            final var macdFuture = macd ? supplyFetch(() -> fetchMacdRaw(symbol, exchange))      : done(null);
            final var s20Future  = s20  ? supplyFetch(() -> fetchSmaRaw(symbol, exchange, 20))   : done(null);
            final var s50Future  = s50  ? supplyFetch(() -> fetchSmaRaw(symbol, exchange, 50))   : done(null);
            final var s200Future = s200 ? supplyFetch(() -> fetchSmaRaw(symbol, exchange, 200))  : done(null);

            CompletableFuture.allOf(rsiFuture, macdFuture, s20Future, s50Future, s200Future).join();
            return AlphaVantageResponseParser.parseTechnicals(
                    rsiFuture.join(), macdFuture.join(), s20Future.join(),
                    s50Future.join(), s200Future.join(), symbol, exchange);
        } catch (CompletionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof IOException) throw (IOException) cause;
            throw new IOException("Technicals fetch failed: " + cause.getMessage(), cause);
        }
    }

    private CompletableFuture<String> supplyFetch(final CheckedSupplier<String> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    private static CompletableFuture<String> done(final String val) {
        return CompletableFuture.completedFuture(val);
    }

    @FunctionalInterface
    interface CheckedSupplier<T> {
        T get() throws IOException;
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
        public RateLimitException(final String message) {
            super(message);
        }
    }
}
