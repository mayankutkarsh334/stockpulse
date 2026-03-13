package com.stockmarket.health;

import com.codahale.metrics.health.HealthCheck;
import com.stockmarket.config.KafkaConfig;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
public class KafkaHealthCheck extends HealthCheck {

    private final KafkaConfig config;

    @Inject
    public KafkaHealthCheck(final KafkaConfig config) {
        this.config = config;
    }

    @Override
    protected Result check() {
        try (final var admin = AdminClient.create(
                Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers()))) {
            admin.listTopics().names().get(5, TimeUnit.SECONDS);
            return Result.healthy();
        } catch (Exception e) {
            return Result.unhealthy("Kafka check failed: " + e.getMessage());
        }
    }
}
