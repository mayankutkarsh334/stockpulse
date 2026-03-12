package com.stockmarket.kafka.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.stockmarket.config.KafkaConfig;
import com.stockmarket.model.cache.StockQuote;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import java.util.Properties;

@Slf4j
public class PriceAlertProducer {

    private final KafkaProducer<String, String> producer;
    private final KafkaConfig config;
    private final ObjectMapper mapper;

    public PriceAlertProducer(KafkaConfig config) {
        this.config = config;
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
        this.producer = new KafkaProducer<>(props);
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public void publish(StockQuote quote) {
        try {
            String key = quote.getSymbol() + ":" + quote.getExchange().name();
            String value = mapper.writeValueAsString(quote);
            ProducerRecord<String, String> record = new ProducerRecord<>(config.getPriceAlertTopic(), key, value);
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
