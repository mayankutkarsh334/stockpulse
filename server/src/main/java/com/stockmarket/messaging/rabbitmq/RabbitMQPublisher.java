package com.stockmarket.messaging.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.stockmarket.config.RabbitMQConfig;
import com.stockmarket.model.entity.PriceAlert;
import io.dropwizard.lifecycle.Managed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Singleton
public class RabbitMQPublisher implements Managed {

    private final RabbitMQConfig config;
    private final ObjectMapper mapper;
    private Connection connection;
    private Channel channel;

    @Inject
    public RabbitMQPublisher(final RabbitMQConfig config, final ObjectMapper mapper) {
        this.config = config;
        this.mapper = mapper;
    }

    @Override
    public void start() throws Exception {
        try {
            final var factory = new ConnectionFactory();
            factory.setHost(config.getHost());
            factory.setPort(config.getPort());
            factory.setUsername(config.getUsername());
            factory.setPassword(config.getPassword());
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.queueDeclare(config.getNotificationQueue(), true, false, false, null);
            log.info("RabbitMQ connected to {}:{}", config.getHost(), config.getPort());
        } catch (Exception e) {
            log.warn("RabbitMQ not available, notifications disabled: {}", e.getMessage());
        }
    }

    @Override
    public void stop() throws Exception {
        try {
            if (channel != null && channel.isOpen()) channel.close();
            if (connection != null && connection.isOpen()) connection.close();
        } catch (Exception e) {
            log.warn("Error closing RabbitMQ connection: {}", e.getMessage());
        }
    }

    public void publishAlertNotification(final PriceAlert alert, final BigDecimal currentPrice) {
        if (channel == null || !channel.isOpen()) {
            log.warn("RabbitMQ channel not available, skipping notification");
            return;
        }
        try {
            final var notification = Map.of(
                    "type", "PRICE_ALERT",
                    "userId", alert.getUserId(),
                    "symbol", alert.getSymbol(),
                    "exchange", alert.getExchange().name(),
                    "direction", alert.getDirection().name(),
                    "targetPrice", alert.getTargetPrice(),
                    "currentPrice", currentPrice,
                    "alertId", alert.getId()
            );
            final var json = mapper.writeValueAsString(notification);
            channel.basicPublish("", config.getNotificationQueue(),
                    null, json.getBytes(StandardCharsets.UTF_8));
            log.info("Published alert notification for {}", alert.getSymbol());
        } catch (Exception e) {
            log.error("Failed to publish RabbitMQ notification: {}", e.getMessage());
        }
    }
}
