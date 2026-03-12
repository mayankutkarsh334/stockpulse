package com.stockmarket.dao.aerospike;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.WritePolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.stockmarket.config.AerospikeConfig;
import com.stockmarket.model.cache.StockQuote;
import com.stockmarket.model.enums.Exchange;
import lombok.extern.slf4j.Slf4j;
import java.util.Optional;

@Slf4j
public class QuoteCache {

    private static final String SET_NAME = "quotes";
    private final AerospikeClient client;
    private final AerospikeConfig config;
    private final ObjectMapper mapper;

    public QuoteCache(AerospikeClient client, AerospikeConfig config) {
        this.client = client;
        this.config = config;
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    private String cacheKey(String symbol, Exchange exchange) {
        return symbol.toUpperCase() + ":" + exchange.name();
    }

    public void put(StockQuote quote) {
        try {
            WritePolicy wp = new WritePolicy();
            wp.expiration = config.getQuoteTtlSeconds();
            Key key = new Key(config.getNamespace(), SET_NAME, cacheKey(quote.getSymbol(), quote.getExchange()));
            String json = mapper.writeValueAsString(quote);
            client.put(wp, key, new Bin("data", json));
        } catch (Exception e) {
            log.warn("Failed to cache quote for {}: {}", quote.getSymbol(), e.getMessage());
        }
    }

    public Optional<StockQuote> get(String symbol, Exchange exchange) {
        try {
            Key key = new Key(config.getNamespace(), SET_NAME, cacheKey(symbol, exchange));
            Record record = client.get(null, key);
            if (record == null) return Optional.empty();
            String json = record.getString("data");
            return Optional.of(mapper.readValue(json, StockQuote.class));
        } catch (Exception e) {
            log.warn("Failed to read quote cache for {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    public void evict(String symbol, Exchange exchange) {
        try {
            Key key = new Key(config.getNamespace(), SET_NAME, cacheKey(symbol, exchange));
            client.delete(null, key);
        } catch (Exception e) {
            log.warn("Failed to evict quote cache for {}: {}", symbol, e.getMessage());
        }
    }
}
