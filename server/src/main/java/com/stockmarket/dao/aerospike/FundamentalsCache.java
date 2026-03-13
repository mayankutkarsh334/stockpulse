package com.stockmarket.dao.aerospike;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.WritePolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmarket.config.AerospikeConfig;
import com.stockmarket.model.cache.StockFundamentals;
import com.stockmarket.model.enums.Exchange;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import java.util.Optional;

@Slf4j
@Singleton
public class FundamentalsCache {

    private static final String SET_NAME = "fundamentals";
    private final AerospikeClient client;
    private final AerospikeConfig config;
    private final ObjectMapper mapper;

    @Inject
    public FundamentalsCache(final AerospikeClient client,
                              final AerospikeConfig config,
                              final ObjectMapper mapper) {
        this.client = client;
        this.config = config;
        this.mapper = mapper;
    }

    private String cacheKey(final String symbol, final Exchange exchange) {
        return symbol.toUpperCase() + ":" + exchange.name();
    }

    public void put(final StockFundamentals fundamentals) {
        try {
            final var wp = new WritePolicy();
            wp.expiration = config.getFundamentalsTtlSeconds();
            final var key = new Key(config.getNamespace(), SET_NAME,
                    cacheKey(fundamentals.getSymbol(), fundamentals.getExchange()));
            final var json = mapper.writeValueAsString(fundamentals);
            client.put(wp, key, new Bin("data", json));
        } catch (Exception e) {
            log.warn("Failed to cache fundamentals for {}: {}", fundamentals.getSymbol(), e.getMessage());
        }
    }

    public Optional<StockFundamentals> get(final String symbol, final Exchange exchange) {
        try {
            final var key = new Key(config.getNamespace(), SET_NAME, cacheKey(symbol, exchange));
            final var record = client.get(null, key);
            if (record == null) return Optional.empty();
            final var json = record.getString("data");
            return Optional.of(mapper.readValue(json, StockFundamentals.class));
        } catch (Exception e) {
            log.warn("Failed to read fundamentals cache for {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }
}
