package com.stockmarket.dao.aerospike;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.WritePolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.stockmarket.config.AerospikeConfig;
import com.stockmarket.model.cache.StockTechnicals;
import com.stockmarket.model.enums.Exchange;
import lombok.extern.slf4j.Slf4j;
import java.util.Optional;

@Slf4j
public class TechnicalsCache {

    private static final String SET_NAME = "technicals";
    private final AerospikeClient client;
    private final AerospikeConfig config;
    private final ObjectMapper mapper;

    public TechnicalsCache(AerospikeClient client, AerospikeConfig config) {
        this.client = client;
        this.config = config;
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    private String cacheKey(String symbol, Exchange exchange) {
        return symbol.toUpperCase() + ":" + exchange.name();
    }

    public void put(StockTechnicals technicals) {
        try {
            WritePolicy wp = new WritePolicy();
            wp.expiration = config.getTechnicalsTtlSeconds();
            Key key = new Key(config.getNamespace(), SET_NAME,
                    cacheKey(technicals.getSymbol(), technicals.getExchange()));
            String json = mapper.writeValueAsString(technicals);
            client.put(wp, key, new Bin("data", json));
        } catch (Exception e) {
            log.warn("Failed to cache technicals for {}: {}", technicals.getSymbol(), e.getMessage());
        }
    }

    public Optional<StockTechnicals> get(String symbol, Exchange exchange) {
        try {
            Key key = new Key(config.getNamespace(), SET_NAME, cacheKey(symbol, exchange));
            Record record = client.get(null, key);
            if (record == null) return Optional.empty();
            String json = record.getString("data");
            return Optional.of(mapper.readValue(json, StockTechnicals.class));
        } catch (Exception e) {
            log.warn("Failed to read technicals cache for {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }
}
