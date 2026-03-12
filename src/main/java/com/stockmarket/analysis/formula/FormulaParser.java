package com.stockmarket.analysis.formula;

/**
 * Recursive descent parser for simple arithmetic formulas.
 * Supports: +, -, *, /, (), metric names (e.g. EPS_GROWTH, RSI_14), numeric literals.
 *
 * Grammar:
 *   expr   = term (('+' | '-') term)*
 *   term   = factor (('*' | '/') factor)*
 *   factor = NUMBER | METRIC_NAME | '(' expr ')'
 */
public class FormulaParser {

    private String input;
    private int pos;

    public FormulaNode parse(String formula) {
        this.input = formula.trim();
        this.pos = 0;
        FormulaNode node = parseExpr();
        skipWhitespace();
        if (pos < input.length()) {
            throw new FormulaException("Unexpected character at position " + pos + ": '" + input.charAt(pos) + "'");
        }
        return node;
    }

    private FormulaNode parseExpr() {
        FormulaNode left = parseTerm();
        while (pos < input.length()) {
            skipWhitespace();
            if (pos < input.length() && (input.charAt(pos) == '+' || input.charAt(pos) == '-')) {
                char op = input.charAt(pos++);
                FormulaNode right = parseTerm();
                left = new FormulaNode.BinaryNode(op, left, right);
            } else {
                break;
            }
        }
        return left;
    }

    private FormulaNode parseTerm() {
        FormulaNode left = parseFactor();
        while (pos < input.length()) {
            skipWhitespace();
            if (pos < input.length() && (input.charAt(pos) == '*' || input.charAt(pos) == '/')) {
                char op = input.charAt(pos++);
                FormulaNode right = parseFactor();
                left = new FormulaNode.BinaryNode(op, left, right);
            } else {
                break;
            }
        }
        return left;
    }

    private FormulaNode parseFactor() {
        skipWhitespace();
        if (pos >= input.length()) {
            throw new FormulaException("Unexpected end of formula at position " + pos);
        }

        char c = input.charAt(pos);

        if (c == '(') {
            pos++; // consume '('
            FormulaNode inner = parseExpr();
            skipWhitespace();
            if (pos >= input.length() || input.charAt(pos) != ')') {
                throw new FormulaException("Missing closing parenthesis");
            }
            pos++; // consume ')'
            return inner;
        }

        if (c == '-' || Character.isDigit(c) || c == '.') {
            return parseNumber();
        }

        if (Character.isLetter(c) || c == '_') {
            return parseMetricName();
        }

        throw new FormulaException("Unexpected character at position " + pos + ": '" + c + "'");
    }

    private FormulaNode parseNumber() {
        int start = pos;
        if (pos < input.length() && input.charAt(pos) == '-') pos++;
        while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) {
            pos++;
        }
        String numStr = input.substring(start, pos);
        try {
            return new FormulaNode.NumberNode(Double.parseDouble(numStr));
        } catch (NumberFormatException e) {
            throw new FormulaException("Invalid number: " + numStr);
        }
    }

    private FormulaNode parseMetricName() {
        int start = pos;
        while (pos < input.length() && (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_')) {
            pos++;
        }
        String name = input.substring(start, pos).toUpperCase();
        return new FormulaNode.MetricNode(name);
    }

    private void skipWhitespace() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
            pos++;
        }
    }

    public static class FormulaException extends RuntimeException {
        public FormulaException(String message) { super(message); }
    }
}
