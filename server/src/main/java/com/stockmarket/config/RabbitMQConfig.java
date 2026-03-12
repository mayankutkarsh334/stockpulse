package com.stockmarket.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RabbitMQConfig {
    @JsonProperty private String host = "localhost";
    @JsonProperty private int port = 5672;
    @JsonProperty private String username = "guest";
    @JsonProperty private String password = "guest";
    @JsonProperty private String notificationQueue = "notifications";
}
