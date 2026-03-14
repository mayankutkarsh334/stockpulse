package com.stockmarket.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmarket.config.KafkaConfig;
import com.stockmarket.model.cache.StockQuote;
import com.stockmarket.model.entity.InvestmentPosition;
import com.stockmarket.service.InvestmentPositionService;
import io.dropwizard.lifecycle.Managed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Singleton
public class TrailingStopMonitor implements Managed, Runnable {

    private final KafkaConsumer<String, String> consumer;
    private final InvestmentPositionService positionService;
    private final ObjectMapper mapper;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread consumerThread;

    @Inject
    public TrailingStopMonitor(final KafkaConfig config,
                                final InvestmentPositionService positionService,
                                final ObjectMapper mapper) {
        this.positionService = positionService;
        this.mapper = mapper;

        final var props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, config.getGroupId() + "-trailing-stop");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        this.consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of(config.getPriceAlertTopic()));
    }

    @Override
    public void run() {
        log.info("TrailingStopMonitor started");
        while (running.get()) {
            try {
                final var records = consumer.poll(Duration.ofMillis(500));
                for (final ConsumerRecord<String, String> record : records) {
                    try {
                        final var quote = mapper.readValue(record.value(), StockQuote.class);
                        final List<InvestmentPosition> positions =
                                positionService.getActivePositionsBySymbol(quote.getSymbol());
                        for (final InvestmentPosition pos : positions) {
                            positionService.updateStopLoss(pos.getId(), quote.getPrice());
                        }
                    } catch (Exception e) {
                        log.error("Error processing trailing stop record: {}", e.getMessage());
                    }
                }
                if (!records.isEmpty()) {
                    consumer.commitSync();
                }
                positionService.checkExpiryRules();
            } catch (Exception e) {
                log.error("TrailingStopMonitor error: {}", e.getMessage());
            }
        }
        consumer.close();
        log.info("TrailingStopMonitor stopped");
    }

    @Override
    public void start() {
        running.set(true);
        consumerThread = new Thread(this, "trailing-stop-monitor");
        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    @Override
    public void stop() {
        running.set(false);
        if (consumerThread != null) {
            consumerThread.interrupt();
        }
    }
}
