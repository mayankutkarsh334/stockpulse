package com.stockmarket.model.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimulationResult {

    private String symbol;
    private String ticker;

    @JsonAlias("data_points_used")
    private int dataPointsUsed;

    @JsonAlias("annualized_vol_pct")
    private double annualizedVolPct;

    @JsonAlias("prob_hit_target_pct")
    private double probHitTargetPct;

    @JsonAlias("prob_stop_hit_pct")
    private double probStopHitPct;

    @JsonAlias("prob_expired_pct")
    private double probExpiredPct;

    @JsonAlias("pct5_gain_pct")
    private double pct5GainPct;

    @JsonAlias("pct50_gain_pct")
    private double pct50GainPct;

    @JsonAlias("pct95_gain_pct")
    private double pct95GainPct;

    @JsonAlias("expected_gain_pct")
    private double expectedGainPct;

    @JsonAlias("arima_30d_forecast_pct")
    private Double arima30dForecastPct;   // nullable

    @JsonAlias("garch_vol_pct")
    private Double garchVolPct;            // nullable

    @JsonAlias("bs_implied_vol_pct")
    private Double bsImpliedVolPct;        // nullable

    @JsonAlias("lstm_30d_forecast_pct")
    private Double lstm30dForecastPct;

    @JsonAlias("xgb_30d_forecast_pct")
    private Double xgb30dForecastPct;

    @JsonAlias("transformer_30d_forecast_pct")
    private Double transformer30dForecastPct;

    @JsonAlias("top_features")
    private List<Map<String, Object>> topFeatures;

    @JsonAlias("num_paths")
    private int numPaths;

    @JsonAlias("horizon_days")
    private int horizonDays;

    @JsonAlias("lstm_price_path")
    private List<Double> lstmPricePath;

    @JsonAlias("xgb_price_path")
    private List<Double> xgbPricePath;

    @JsonAlias("transformer_price_path")
    private List<Double> transformerPricePath;

    @JsonAlias("historical_closes")
    private List<Double> historicalCloses;

    @JsonAlias("extra_feature_names")
    private List<String> extraFeatureNames;
}
