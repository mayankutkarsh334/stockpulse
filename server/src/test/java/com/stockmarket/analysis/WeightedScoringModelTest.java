package com.stockmarket.analysis;

import com.stockmarket.analysis.model.WeightedScoringModel;
import com.stockmarket.model.enums.Exchange;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class WeightedScoringModelTest {

    private final WeightedScoringModel model = new WeightedScoringModel();

    private MetricSnapshot makeSnapshot(String symbol, Double pe, Double epsGrowth, Double rsi) {
        return new MetricSnapshot(symbol, Exchange.NSE,
                pe, null, epsGrowth, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                rsi, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    void testRankingOrderWithWeights() {
        List<MetricSnapshot> snapshots = List.of(
                makeSnapshot("GOOD", 10.0, 0.30, 40.0),
                makeSnapshot("BAD", 30.0, 0.05, 70.0),
                makeSnapshot("MID", 20.0, 0.15, 55.0)
        );

        Map<String, Object> params = Map.of(
                "weights", Map.of("EPS_GROWTH", 0.6, "RSI_14", 0.4));

        List<StockScore> result = model.analyze(snapshots, params);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).rank()).isEqualTo(1);
        assertThat(result.get(0).symbol()).isEqualTo("GOOD");
    }

    @Test
    void testAllSameValuesGetEqualScore() {
        List<MetricSnapshot> snapshots = List.of(
                makeSnapshot("A", 15.0, 0.10, 50.0),
                makeSnapshot("B", 15.0, 0.10, 50.0)
        );
        Map<String, Object> params = Map.of("weights", Map.of("PE", 0.5, "RSI_14", 0.5));
        List<StockScore> result = model.analyze(snapshots, params);
        assertThat(result.get(0).score()).isEqualTo(result.get(1).score(), withPrecision(0.001));
    }
}
