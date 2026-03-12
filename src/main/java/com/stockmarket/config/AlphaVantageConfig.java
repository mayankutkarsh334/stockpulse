package com.stockmarket.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlphaVantageConfig {
    @JsonProperty private String apiKey = "";
    @JsonProperty private String baseUrl = "https://www.alphavantage.co/query";
    @JsonProperty private int rateLimitPerMinute = 5;
    @JsonProperty private int dailyCallLimit = 25;
}
