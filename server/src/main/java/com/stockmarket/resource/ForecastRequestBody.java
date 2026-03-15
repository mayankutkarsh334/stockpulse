package com.stockmarket.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ForecastRequestBody {
    private String exchange = "NSE";
    @JsonProperty("entryPrice")      private Double entryPrice;
    @JsonProperty("stopLoss")        private Double stopLoss;
    @JsonProperty("targetPct")       private Double targetPct;
    @JsonProperty("extraIndicators") private List<Map<String, Object>> extraIndicators;
}
