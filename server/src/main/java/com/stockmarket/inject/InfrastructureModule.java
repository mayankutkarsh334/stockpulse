package com.stockmarket.inject;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.policy.ClientPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.stockmarket.MarketServiceConfiguration;
import com.stockmarket.client.AlphaVantageClient;
import com.stockmarket.client.MlServiceClient;
import com.stockmarket.config.AerospikeConfig;
import com.stockmarket.config.AlphaVantageConfig;
import com.stockmarket.config.KafkaConfig;
import com.stockmarket.config.MlServiceConfig;
import com.stockmarket.config.RabbitMQConfig;
import com.stockmarket.messaging.rabbitmq.RabbitMQPublisher;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InfrastructureModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(AlphaVantageClient.class).in(Singleton.class);
        bind(MlServiceClient.class).in(Singleton.class);
        bind(RabbitMQPublisher.class).in(Singleton.class);
    }

    @Provides
    @Singleton
    ObjectMapper provideObjectMapper(final Environment environment) {
        return environment.getObjectMapper().registerModule(new JavaTimeModule());
    }

    @Provides
    @Singleton
    AerospikeConfig provideAerospikeConfig(final MarketServiceConfiguration config) {
        return config.getAerospike();
    }

    @Provides
    @Singleton
    AlphaVantageConfig provideAlphaVantageConfig(final MarketServiceConfiguration config) {
        return config.getAlphaVantage();
    }

    @Provides
    @Singleton
    KafkaConfig provideKafkaConfig(final MarketServiceConfiguration config) {
        return config.getKafka();
    }

    @Provides
    @Singleton
    RabbitMQConfig provideRabbitMQConfig(final MarketServiceConfiguration config) {
        return config.getRabbitMQ();
    }

    @Provides
    @Singleton
    MlServiceConfig provideMlServiceConfig(final MarketServiceConfiguration config) {
        return config.getMlService();
    }

    @Provides
    @Singleton
    ExecutorService provideAnalysisExecutor(final Environment environment) {
        final var executor = Executors.newFixedThreadPool(30);
        environment.lifecycle().manage(new Managed() {
            @Override
            public void start() {
            }

            @Override
            public void stop() {
                executor.shutdown();
            }
        });
        return executor;
    }

    @Provides
    @Singleton
    AerospikeClient provideAerospikeClient(final AerospikeConfig aeroCfg, final Environment environment) {
        final var clientPolicy = new ClientPolicy();
        final var client = new AerospikeClient(clientPolicy,
                aeroCfg.getHosts()[0].getHost(), aeroCfg.getHosts()[0].getPort());
        environment.lifecycle().manage(new Managed() {
            @Override
            public void start() {
            }

            @Override
            public void stop() {
                client.close();
            }
        });
        return client;
    }
}
