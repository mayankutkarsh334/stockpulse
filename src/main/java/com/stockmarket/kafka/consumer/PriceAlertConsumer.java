package com.stockmarket.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.stockmarket.config.KafkaConfig;
import com.stockmarket.model.cache.StockQuote;
import com.stockmarket.service.AlertService;
import io.dropwizard.lifecycle.Managed;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class PriceAlertConsumer implements Managed, Runnable {

    private final KafkaConsumer<String, String> consumer;
    private final AlertService alertService;
    private final ObjectMapper mapper;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread consumerThread;

    public PriceAlertConsumer(KafkaConfig config, AlertService alertService) {
        this.alertService = alertService;
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, config.getGroupId() + "-alert");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        this.consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of(config.getPriceAlertTopic()));
    }

    @Override
    public void run() {
        log.info("PriceAlertConsumer started");
        while (running.get()) {
            try {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    try {
                        StockQuote quote = mapper.readValue(record.value(), StockQuote.class);
                        alertService.processQuoteForAlerts(quote);
                    } catch (Exception e) {
                        log.error("Error processing alert record: {}", e.getMessage());
                    }
                }
                if (!records.isEmpty()) {
                    consumer.commitSync();
                }
            } catch (Exception e) {
                log.error("PriceAlertConsumer error: {}", e.getMessage());
            }
        }
        consumer.close();
        log.info("PriceAlertConsumer stopped");
    }

    @Override
    public void start() {
        running.set(true);
        consumerThread = new Thread(this, "price-alert-consumer");
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
