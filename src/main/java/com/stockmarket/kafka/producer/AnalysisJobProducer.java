package com.stockmarket.kafka.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmarket.config.KafkaConfig;
import com.stockmarket.model.dto.request.AnalysisRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import java.util.Properties;
import java.util.UUID;

@Slf4j
public class AnalysisJobProducer {

    private final KafkaProducer<String, String> producer;
    private final KafkaConfig config;
    private final ObjectMapper mapper = new ObjectMapper();

    public AnalysisJobProducer(KafkaConfig config) {
        this.config = config;
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        this.producer = new KafkaProducer<>(props);
    }

    public String publishAnalysisJob(AnalysisRequest req) {
        String jobId = UUID.randomUUID().toString();
        try {
            String value = mapper.writeValueAsString(req);
            ProducerRecord<String, String> record = new ProducerRecord<>(config.getAnalysisJobTopic(), jobId, value);
            producer.send(record, (metadata, ex) -> {
                if (ex != null) log.error("Failed to publish analysis job: {}", ex.getMessage());
                else log.info("Analysis job {} enqueued to partition {}", jobId, metadata.partition());
            });
        } catch (Exception e) {
            log.error("Error publishing analysis job: {}", e.getMessage());
        }
        return jobId;
    }

    public void close() {
        producer.close();
    }
}
