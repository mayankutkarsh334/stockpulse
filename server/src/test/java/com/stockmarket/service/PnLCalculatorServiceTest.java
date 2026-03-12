package com.stockmarket.service;

import com.stockmarket.model.cache.StockQuote;
import com.stockmarket.model.entity.Holding;
import com.stockmarket.model.enums.Currency;
import com.stockmarket.model.enums.Exchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PnLCalculatorServiceTest {

    @Mock private QuoteService quoteService;
    private PnLCalculatorService pnlService;

    @BeforeEach
    void setup() {
        pnlService = new PnLCalculatorService(quoteService);
    }

    @Test
    void testWeightedAveragePriceBuy() {
        BigDecimal newAvg = pnlService.computeNewAveragePrice(
                new BigDecimal("10"), new BigDecimal("100.00"),
                new BigDecimal("5"), new BigDecimal("120.00"));
        // (10*100 + 5*120) / 15 = 1600/15 = 106.6667
        assertThat(newAvg).isEqualByComparingTo(new BigDecimal("106.6667"));
    }

    @Test
    void testWeightedAveragePriceZeroInitialQty() {
        BigDecimal newAvg = pnlService.computeNewAveragePrice(
                BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("10"), new BigDecimal("150.00"));
        assertThat(newAvg).isEqualByComparingTo(new BigDecimal("150.0000"));
    }

    @Test
    void testComputeHoldingPnLPositive() throws Exception {
        Holding holding = Holding.builder()
                .id("h1").portfolioId("p1").symbol("TCS").exchange(Exchange.NSE)
                .quantity(new BigDecimal("10")).averageBuyPrice(new BigDecimal("3000.00"))
                .currency(Currency.INR).build();

        StockQuote quote = StockQuote.builder()
                .symbol("TCS").exchange(Exchange.NSE)
                .price(new BigDecimal("3500.00"))
                .change(BigDecimal.ZERO).changePercent(BigDecimal.ZERO)
                .timestamp(Instant.now()).build();

        when(quoteService.getQuote("TCS", Exchange.NSE)).thenReturn(quote);

        var result = pnlService.computeHoldingPnL(holding);
        assertThat(result.getPnl()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(result.getPnlPercent()).isEqualByComparingTo(new BigDecimal("16.67"));
    }

    @Test
    void testComputeHoldingPnLNegative() throws Exception {
        Holding holding = Holding.builder()
                .id("h2").portfolioId("p1").symbol("INFY").exchange(Exchange.NSE)
                .quantity(new BigDecimal("5")).averageBuyPrice(new BigDecimal("1500.00"))
                .currency(Currency.INR).build();

        StockQuote quote = StockQuote.builder()
                .symbol("INFY").exchange(Exchange.NSE)
                .price(new BigDecimal("1400.00"))
                .change(BigDecimal.ZERO).changePercent(BigDecimal.ZERO)
                .timestamp(Instant.now()).build();

        when(quoteService.getQuote("INFY", Exchange.NSE)).thenReturn(quote);

        var result = pnlService.computeHoldingPnL(holding);
        assertThat(result.getPnl()).isEqualByComparingTo(new BigDecimal("-500.00"));
        assertThat(result.getPnlPercent()).isNegative();
    }
}
