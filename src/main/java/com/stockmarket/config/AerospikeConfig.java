package com.stockmarket.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AerospikeConfig {
    @JsonProperty private AerospikeHostConfig[] hosts = {new AerospikeHostConfig()};
    @JsonProperty private String namespace = "stocks";
    @JsonProperty private int quoteTtlSeconds = 900;
    @JsonProperty private int fundamentalsTtlSeconds = 86400;
    @JsonProperty private int technicalsTtlSeconds = 3600;

    @Getter
    @Setter
    public static class AerospikeHostConfig {
        @JsonProperty private String host = "localhost";
        @JsonProperty private int port = 3000;
    }
}
