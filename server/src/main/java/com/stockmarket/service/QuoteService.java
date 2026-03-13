package com.stockmarket.service;

import com.stockmarket.client.AlphaVantageClient;
import com.stockmarket.dao.aerospike.QuoteCache;
import com.stockmarket.kafka.producer.PriceAlertProducer;
import com.stockmarket.model.cache.StockQuote;
import com.stockmarket.model.enums.Exchange;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.Optional;

@Slf4j
@Singleton
public class QuoteService {

    private final AlphaVantageClient avClient;
    private final QuoteCache quoteCache;
    private final PriceAlertProducer priceAlertProducer;

    @Inject
    public QuoteService(final AlphaVantageClient avClient,
                        final QuoteCache quoteCache,
                        final PriceAlertProducer priceAlertProducer) {
        this.avClient = avClient;
        this.quoteCache = quoteCache;
        this.priceAlertProducer = priceAlertProducer;
    }

    public StockQuote getQuote(final String symbol, final Exchange exchange) throws IOException {
        final var cached = quoteCache.get(symbol, exchange);
        if (cached.isPresent()) {
            log.debug("Cache hit for quote {}/{}", symbol, exchange);
            return cached.get();
        }
        log.debug("Cache miss for quote {}/{}, fetching from Alpha Vantage", symbol, exchange);
        final var quote = avClient.fetchQuote(symbol, exchange);
        quoteCache.put(quote);
        // Publish to Kafka so PriceAlertConsumer can check triggers
        priceAlertProducer.publish(quote);
        return quote;
    }

    public Optional<StockQuote> getCachedQuote(final String symbol, final Exchange exchange) {
        return quoteCache.get(symbol, exchange);
    }
}
