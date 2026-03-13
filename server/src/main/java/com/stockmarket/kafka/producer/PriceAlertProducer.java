package com.stockmarket.kafka.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmarket.config.KafkaConfig;
import com.stockmarket.model.cache.StockQuote;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import java.util.Properties;

@Slf4j
@Singleton
public class PriceAlertProducer {

    private final KafkaProducer<String, String> producer;
    private final KafkaConfig config;
    private final ObjectMapper mapper;

    @Inject
    public PriceAlertProducer(final KafkaConfig config, final ObjectMapper mapper) {
        this.config = config;
        this.mapper = mapper;
        final var props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
        this.producer = new KafkaProducer<>(props);
    }

    public void publish(final StockQuote quote) {
        try {
            final var key = quote.getSymbol() + ":" + quote.getExchange().name();
            final var value = mapper.writeValueAsString(quote);
            final var record = new ProducerRecord<>(config.getPriceAlertTopic(), key, value);
            producer.send(record, (metadata, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish quote to Kafka: {}", ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing quote: {}", e.getMessage());
        }
    }

    public void close() {
        producer.close();
    }
}
