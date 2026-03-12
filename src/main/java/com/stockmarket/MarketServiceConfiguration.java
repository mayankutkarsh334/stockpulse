package com.stockmarket;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stockmarket.config.*;
import io.dropwizard.core.Configuration;
import io.dropwizard.db.DataSourceFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarketServiceConfiguration extends Configuration {

    @Valid @NotNull @JsonProperty
    private DataSourceFactory database = new DataSourceFactory();

    @Valid @NotNull @JsonProperty
    private AlphaVantageConfig alphaVantage = new AlphaVantageConfig();

    @Valid @NotNull @JsonProperty
    private AerospikeConfig aerospike = new AerospikeConfig();

    @Valid @NotNull @JsonProperty
    private KafkaConfig kafka = new KafkaConfig();

    @Valid @NotNull @JsonProperty
    private RabbitMQConfig rabbitMQ = new RabbitMQConfig();
}
