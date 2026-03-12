package com.stockmarket.model.enums;

public enum Exchange {
    NSE, BSE, NYSE, NASDAQ;

    public String toAlphaVantageSuffix() {
        return switch (this) {
            case NSE -> ".NSE";
            case BSE -> ".BSE";
            case NYSE, NASDAQ -> "";
        };
    }

    public boolean isIndian() {
        return this == NSE || this == BSE;
    }
}
