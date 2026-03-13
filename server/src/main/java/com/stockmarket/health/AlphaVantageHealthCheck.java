package com.stockmarket.health;

import com.codahale.metrics.health.HealthCheck;
import com.stockmarket.client.AlphaVantageClient;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class AlphaVantageHealthCheck extends HealthCheck {

    private final AlphaVantageClient client;

    @Inject
    public AlphaVantageHealthCheck(final AlphaVantageClient client) {
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
