package com.stockmarket.analysis.formula;

import com.stockmarket.analysis.MetricSnapshot;

public class FormulaEvaluator {

    public double evaluate(FormulaNode node, MetricSnapshot snapshot) {
        return switch (node) {
            case FormulaNode.NumberNode n -> n.value();
            case FormulaNode.MetricNode m -> {
                Double val = snapshot.getByName(m.name());
                if (val == null) {
                    throw new FormulaParser.FormulaException(
                            "Unknown or null metric '" + m.name() + "' for symbol " + snapshot.symbol());
                }
                yield val;
            }
            case FormulaNode.BinaryNode b -> {
                double left = evaluate(b.left(), snapshot);
                double right = evaluate(b.right(), snapshot);
                yield switch (b.operator()) {
                    case '+' -> left + right;
                    case '-' -> left - right;
                    case '*' -> left * right;
                    case '/' -> {
                        if (Math.abs(right) < 1e-10) {
                            throw new FormulaParser.FormulaException("Division by zero in formula");
                        }
                        yield left / right;
                    }
                    default -> throw new FormulaParser.FormulaException("Unknown operator: " + b.operator());
                };
            }
        };
    }
}
