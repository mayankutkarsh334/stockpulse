package com.stockmarket.health;

import com.aerospike.client.AerospikeClient;
import com.codahale.metrics.health.HealthCheck;

public class AerospikeHealthCheck extends HealthCheck {
    private final AerospikeClient client;

    public AerospikeHealthCheck(AerospikeClient client) {
        this.client = client;
    }

    @Override
    protected Result check() {
        try {
            boolean connected = client.isConnected();
            return connected ? Result.healthy() : Result.unhealthy("Aerospike not connected");
        } catch (Exception e) {
            return Result.unhealthy("Aerospike check failed: " + e.getMessage());
        }
    }
}
