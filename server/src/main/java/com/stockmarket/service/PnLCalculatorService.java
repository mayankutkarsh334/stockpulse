package com.stockmarket.service;

import com.stockmarket.model.dto.response.HoldingResponse;
import com.stockmarket.model.entity.Holding;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Singleton
public class PnLCalculatorService {

    private final QuoteService quoteService;

    @Inject
    public PnLCalculatorService(final QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    public HoldingResponse computeHoldingPnL(final Holding holding) {
        BigDecimal currentPrice;
        try {
            final var quote = quoteService.getQuote(holding.getSymbol(), holding.getExchange());
            currentPrice = quote.getPrice();
        } catch (IOException e) {
            log.warn("Could not fetch quote for {}/{}: {}", holding.getSymbol(), holding.getExchange(), e.getMessage());
            currentPrice = holding.getAverageBuyPrice(); // fallback: no P&L
        }

        final var investedValue = holding.getAverageBuyPrice().multiply(holding.getQuantity());
        final var currentValue = currentPrice.multiply(holding.getQuantity());
        final var pnl = currentValue.subtract(investedValue);
        final var pnlPercent = investedValue.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : pnl.divide(investedValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        return HoldingResponse.builder()
                .id(holding.getId())
                .symbol(holding.getSymbol())
                .exchange(holding.getExchange())
                .quantity(holding.getQuantity())
                .averageBuyPrice(holding.getAverageBuyPrice())
                .currentPrice(currentPrice)
                .investedValue(investedValue.setScale(2, RoundingMode.HALF_UP))
                .currentValue(currentValue.setScale(2, RoundingMode.HALF_UP))
                .pnl(pnl.setScale(2, RoundingMode.HALF_UP))
                .pnlPercent(pnlPercent.setScale(2, RoundingMode.HALF_UP))
                .currency(holding.getCurrency())
                .build();
    }

    /**
     * Weighted average price for BUY: newAvg = (currentQty * currentAvg + newQty * txnPrice) / (currentQty + newQty)
     */
    public BigDecimal computeNewAveragePrice(final BigDecimal currentQty, final BigDecimal currentAvg,
                                              final BigDecimal newQty, final BigDecimal txnPrice) {
        final var totalQty = currentQty.add(newQty);
        if (totalQty.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        final var totalCost = currentQty.multiply(currentAvg).add(newQty.multiply(txnPrice));
        return totalCost.divide(totalQty, 4, RoundingMode.HALF_UP);
    }
}
