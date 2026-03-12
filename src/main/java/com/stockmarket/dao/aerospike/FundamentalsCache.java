package com.stockmarket.dao.aerospike;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.WritePolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.stockmarket.config.AerospikeConfig;
import com.stockmarket.model.cache.StockFundamentals;
import com.stockmarket.model.enums.Exchange;
import lombok.extern.slf4j.Slf4j;
import java.util.Optional;

@Slf4j
public class FundamentalsCache {

    private static final String SET_NAME = "fundamentals";
    private final AerospikeClient client;
    private final AerospikeConfig config;
    private final ObjectMapper mapper;

    public FundamentalsCache(AerospikeClient client, AerospikeConfig config) {
        this.client = client;
        this.config = config;
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    private String cacheKey(String symbol, Exchange exchange) {
        return symbol.toUpperCase() + ":" + exchange.name();
    }

    public void put(StockFundamentals fundamentals) {
        try {
            WritePolicy wp = new WritePolicy();
            wp.expiration = config.getFundamentalsTtlSeconds();
            Key key = new Key(config.getNamespace(), SET_NAME,
                    cacheKey(fundamentals.getSymbol(), fundamentals.getExchange()));
            String json = mapper.writeValueAsString(fundamentals);
            client.put(wp, key, new Bin("data", json));
        } catch (Exception e) {
            log.warn("Failed to cache fundamentals for {}: {}", fundamentals.getSymbol(), e.getMessage());
        }
    }

    public Optional<StockFundamentals> get(String symbol, Exchange exchange) {
        try {
            Key key = new Key(config.getNamespace(), SET_NAME, cacheKey(symbol, exchange));
            Record record = client.get(null, key);
            if (record == null) return Optional.empty();
            String json = record.getString("data");
            return Optional.of(mapper.readValue(json, StockFundamentals.class));
        } catch (Exception e) {
            log.warn("Failed to read fundamentals cache for {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }
}
