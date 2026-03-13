package com.stockmarket.health;

import com.aerospike.client.AerospikeClient;
import com.codahale.metrics.health.HealthCheck;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class AerospikeHealthCheck extends HealthCheck {

    private final AerospikeClient client;

    @Inject
    public AerospikeHealthCheck(final AerospikeClient client) {
        this.client = client;
    }

    @Override
    protected Result check() {
        try {
            final var connected = client.isConnected();
            return connected ? Result.healthy() : Result.unhealthy("Aerospike not connected");
        } catch (Exception e) {
            return Result.unhealthy("Aerospike check failed: " + e.getMessage());
        }
    }
}
