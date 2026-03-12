package com.stockmarket.service;

import com.stockmarket.model.cache.StockQuote;
import com.stockmarket.model.dto.response.HoldingResponse;
import com.stockmarket.model.entity.Holding;
import com.stockmarket.model.enums.Exchange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@RequiredArgsConstructor
public class PnLCalculatorService {

    private final QuoteService quoteService;

    public HoldingResponse computeHoldingPnL(Holding holding) {
        BigDecimal currentPrice;
        try {
            StockQuote quote = quoteService.getQuote(holding.getSymbol(), holding.getExchange());
            currentPrice = quote.getPrice();
        } catch (IOException e) {
            log.warn("Could not fetch quote for {}/{}: {}", holding.getSymbol(), holding.getExchange(), e.getMessage());
            currentPrice = holding.getAverageBuyPrice(); // fallback: no P&L
        }

        BigDecimal investedValue = holding.getAverageBuyPrice().multiply(holding.getQuantity());
        BigDecimal currentValue = currentPrice.multiply(holding.getQuantity());
        BigDecimal pnl = currentValue.subtract(investedValue);
        BigDecimal pnlPercent = investedValue.compareTo(BigDecimal.ZERO) == 0
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
    public BigDecimal computeNewAveragePrice(BigDecimal currentQty, BigDecimal currentAvg,
                                              BigDecimal newQty, BigDecimal txnPrice) {
        BigDecimal totalQty = currentQty.add(newQty);
        if (totalQty.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        BigDecimal totalCost = currentQty.multiply(currentAvg).add(newQty.multiply(txnPrice));
        return totalCost.divide(totalQty, 4, RoundingMode.HALF_UP);
    }
}
