package com.stockmarket.health;

import com.codahale.metrics.health.HealthCheck;
import com.stockmarket.client.AlphaVantageClient;

public class AlphaVantageHealthCheck extends HealthCheck {
    private final AlphaVantageClient client;

    public AlphaVantageHealthCheck(AlphaVantageClient client) {
        this.client = client;
    }

    @Override
    protected Result check() {
        if (client.isHealthy()) {
            return Result.healthy("Circuit breaker CLOSED. Daily calls: " + client.getDailyCallCount());
        }
        return Result.unhealthy("Alpha Vantage circuit breaker is OPEN");
    }
}
