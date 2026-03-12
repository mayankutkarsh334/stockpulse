package com.stockmarket.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KafkaConfig {
    @JsonProperty private String bootstrapServers = "localhost:9092";
    @JsonProperty private String priceAlertTopic = "stock-price-alerts";
    @JsonProperty private String analysisJobTopic = "analysis-jobs";
    @JsonProperty private String groupId = "stock-investing-service";
}
