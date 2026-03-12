package com.stockmarket.analysis;

import com.stockmarket.analysis.formula.FormulaEvaluator;
import com.stockmarket.analysis.formula.FormulaNode;
import com.stockmarket.analysis.formula.FormulaParser;
import com.stockmarket.model.enums.Exchange;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class FormulaParserTest {

    private final FormulaParser parser = new FormulaParser();
    private final FormulaEvaluator evaluator = new FormulaEvaluator();

    private MetricSnapshot snapshotWith(Double eps, Double pe, Double rsi) {
        return new MetricSnapshot("TEST", Exchange.NSE,
                pe, eps, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                rsi, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    void testSimpleNumber() {
        FormulaNode node = parser.parse("42.5");
        double result = evaluator.evaluate(node, snapshotWith(null, null, null));
        assertThat(result).isEqualTo(42.5);
    }

    @Test
    void testSingleMetric() {
        FormulaNode node = parser.parse("PE");
        double result = evaluator.evaluate(node, snapshotWith(null, 20.0, null));
        assertThat(result).isEqualTo(20.0);
    }

    @Test
    void testAddition() {
        FormulaNode node = parser.parse("PE + EPS");
        double result = evaluator.evaluate(node, snapshotWith(5.0, 20.0, null));
        assertThat(result).isEqualTo(25.0);
    }

    @Test
    void testMultiplication() {
        FormulaNode node = parser.parse("EPS * 2.0");
        double result = evaluator.evaluate(node, snapshotWith(5.0, null, null));
        assertThat(result).isEqualTo(10.0);
    }

    @Test
    void testDivision() {
        FormulaNode node = parser.parse("EPS / PE");
        double result = evaluator.evaluate(node, snapshotWith(10.0, 20.0, null));
        assertThat(result).isEqualTo(0.5);
    }

    @Test
    void testParentheses() {
        FormulaNode node = parser.parse("(EPS + 5.0) * 2.0");
        double result = evaluator.evaluate(node, snapshotWith(5.0, null, null));
        assertThat(result).isEqualTo(20.0);
    }

    @Test
    void testComplexFormula() {
        FormulaNode node = parser.parse("(EPS / PE) * RSI_14");
        double result = evaluator.evaluate(node, snapshotWith(10.0, 25.0, 50.0));
        assertThat(result).isEqualTo(20.0, withPrecision(0.0001));
    }

    @Test
    void testDivisionByZeroThrows() {
        FormulaNode node = parser.parse("PE / 0");
        assertThatThrownBy(() -> evaluator.evaluate(node, snapshotWith(null, 20.0, null)))
                .isInstanceOf(FormulaParser.FormulaException.class)
                .hasMessageContaining("Division by zero");
    }

    @Test
    void testUnknownMetricThrows() {
        FormulaNode node = parser.parse("UNKNOWN_METRIC");
        assertThatThrownBy(() -> evaluator.evaluate(node, snapshotWith(null, null, null)))
                .isInstanceOf(FormulaParser.FormulaException.class);
    }

    @Test
    void testInvalidFormulaThrows() {
        assertThatThrownBy(() -> parser.parse("PE +"))
                .isInstanceOf(FormulaParser.FormulaException.class);
    }
}
