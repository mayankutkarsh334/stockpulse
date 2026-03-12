package com.stockmarket.analysis.formula;

public sealed interface FormulaNode permits
        FormulaNode.NumberNode,
        FormulaNode.MetricNode,
        FormulaNode.BinaryNode {

    record NumberNode(double value) implements FormulaNode {}

    record MetricNode(String name) implements FormulaNode {}

    record BinaryNode(char operator, FormulaNode left, FormulaNode right) implements FormulaNode {}
}
