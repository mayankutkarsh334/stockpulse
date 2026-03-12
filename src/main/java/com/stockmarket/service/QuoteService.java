package com.stockmarket.service;

import com.stockmarket.client.AlphaVantageClient;
import com.stockmarket.dao.aerospike.QuoteCache;
import com.stockmarket.kafka.producer.PriceAlertProducer;
import com.stockmarket.model.cache.StockQuote;
import com.stockmarket.model.enums.Exchange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class QuoteService {

    private final AlphaVantageClient avClient;
    private final QuoteCache quoteCache;
    private final PriceAlertProducer priceAlertProducer;

    public StockQuote getQuote(String symbol, Exchange exchange) throws IOException {
        Optional<StockQuote> cached = quoteCache.get(symbol, exchange);
        if (cached.isPresent()) {
            log.debug("Cache hit for quote {}/{}", symbol, exchange);
            return cached.get();
        }
        log.debug("Cache miss for quote {}/{}, fetching from Alpha Vantage", symbol, exchange);
        StockQuote quote = avClient.fetchQuote(symbol, exchange);
        quoteCache.put(quote);
        // Publish to Kafka so PriceAlertConsumer can check triggers
        priceAlertProducer.publish(quote);
        return quote;
    }

    public Optional<StockQuote> getCachedQuote(String symbol, Exchange exchange) {
        return quoteCache.get(symbol, exchange);
    }
}
