package com.stockmarket.messaging.rabbitmq;

import com.rabbitmq.client.*;
import com.stockmarket.config.RabbitMQConfig;
import io.dropwizard.lifecycle.Managed;
import lombok.extern.slf4j.Slf4j;
import java.nio.charset.StandardCharsets;

@Slf4j
public class NotificationConsumer implements Managed {

    private final RabbitMQConfig config;
    private Connection connection;
    private Channel channel;

    public NotificationConsumer(RabbitMQConfig config) {
        this.config = config;
    }

    @Override
    public void start() throws Exception {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(config.getHost());
            factory.setPort(config.getPort());
            factory.setUsername(config.getUsername());
            factory.setPassword(config.getPassword());
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.queueDeclare(config.getNotificationQueue(), true, false, false, null);
            channel.basicConsume(config.getNotificationQueue(), true, (tag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                log.info("Notification received: {}", message);
                // Extend: send email, push notification, etc.
            }, tag -> log.warn("Consumer cancelled: {}", tag));
        } catch (Exception e) {
            log.warn("NotificationConsumer not started (RabbitMQ unavailable): {}", e.getMessage());
        }
    }

    @Override
    public void stop() throws Exception {
        try {
            if (channel != null && channel.isOpen()) channel.close();
            if (connection != null && connection.isOpen()) connection.close();
        } catch (Exception e) {
            log.warn("Error closing NotificationConsumer: {}", e.getMessage());
        }
    }
}
