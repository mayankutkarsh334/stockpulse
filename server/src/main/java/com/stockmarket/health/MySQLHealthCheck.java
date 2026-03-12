package com.stockmarket.health;

import com.codahale.metrics.health.HealthCheck;
import org.jdbi.v3.core.Jdbi;

public class MySQLHealthCheck extends HealthCheck {
    private final Jdbi jdbi;

    public MySQLHealthCheck(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    protected Result check() {
        try {
            jdbi.withHandle(handle -> handle.select("SELECT 1").mapTo(Integer.class).one());
            return Result.healthy();
        } catch (Exception e) {
            return Result.unhealthy("MySQL check failed: " + e.getMessage());
        }
    }
}
