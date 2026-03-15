package com.stockmarket.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MlServiceConfig {
    @JsonProperty private String baseUrl = "http://localhost:8081";
}
