package com.stockmarket.service;

import com.stockmarket.dao.mysql.HoldingDao;
import com.stockmarket.dao.mysql.PortfolioDao;
import com.stockmarket.dao.mysql.TransactionDao;
import com.stockmarket.model.dto.request.AddTransactionRequest;
import com.stockmarket.model.dto.request.CreatePortfolioRequest;
import com.stockmarket.model.dto.response.HoldingResponse;
import com.stockmarket.model.dto.response.PortfolioResponse;
import com.stockmarket.model.entity.Holding;
import com.stockmarket.model.entity.Portfolio;
import com.stockmarket.model.entity.Transaction;
import com.stockmarket.model.enums.TransactionType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class PortfolioService {

    private final PortfolioDao portfolioDao;
    private final HoldingDao holdingDao;
    private final TransactionDao transactionDao;
    private final PnLCalculatorService pnlCalculatorService;

    @Inject
    public PortfolioService(final PortfolioDao portfolioDao,
                            final HoldingDao holdingDao,
                            final TransactionDao transactionDao,
                            final PnLCalculatorService pnlCalculatorService) {
        this.portfolioDao = portfolioDao;
        this.holdingDao = holdingDao;
        this.transactionDao = transactionDao;
        this.pnlCalculatorService = pnlCalculatorService;
    }

    public Portfolio createPortfolio(final CreatePortfolioRequest req) {
        final var portfolio = Portfolio.builder()
                .id(UUID.randomUUID().toString())
                .userId(req.getUserId())
                .name(req.getName())
                .currency(req.getCurrency())
                .build();
        portfolioDao.insert(portfolio);
        return portfolio;
    }

    public PortfolioResponse getPortfolioWithPnL(final String portfolioId) {
        final var portfolio = portfolioDao.findById(portfolioId)
                .orElseThrow(() -> new NotFoundException("Portfolio not found: " + portfolioId));

        final var holdings = holdingDao.findByPortfolioId(portfolioId);
        final var holdingResponses = holdings.stream()
                .map(pnlCalculatorService::computeHoldingPnL)
                .collect(Collectors.toList());

        final var totalInvested = holdingResponses.stream()
                .map(HoldingResponse::getInvestedValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        final var totalCurrentValue = holdingResponses.stream()
                .map(HoldingResponse::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        final var totalPnl = totalCurrentValue.subtract(totalInvested);
        final var totalPnlPercent = totalInvested.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : totalPnl.divide(totalInvested, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        return PortfolioResponse.builder()
                .id(portfolio.getId())
                .userId(portfolio.getUserId())
                .name(portfolio.getName())
                .currency(portfolio.getCurrency())
                .totalInvested(totalInvested)
                .totalCurrentValue(totalCurrentValue)
                .totalPnl(totalPnl)
                .totalPnlPercent(totalPnlPercent)
                .holdings(holdingResponses)
                .createdAt(portfolio.getCreatedAt())
                .build();
    }

    public Transaction addTransaction(final String portfolioId, final AddTransactionRequest req) {
        final var portfolio = portfolioDao.findById(portfolioId)
                .orElseThrow(() -> new NotFoundException("Portfolio not found: " + portfolioId));

        final var existingHolding = holdingDao.findByPortfolioSymbolExchange(
                portfolioId, req.getSymbol(), req.getExchange().name());

        String holdingId;
        if (existingHolding.isEmpty()) {
            if (req.getType() == TransactionType.SELL) {
                throw new IllegalArgumentException("Cannot SELL a holding you don't have");
            }
            final var holding = Holding.builder()
                    .id(UUID.randomUUID().toString())
                    .portfolioId(portfolioId)
                    .symbol(req.getSymbol())
                    .exchange(req.getExchange())
                    .quantity(req.getQuantity())
                    .averageBuyPrice(req.getPrice())
                    .currency(portfolio.getCurrency())
                    .build();
            holdingDao.insert(holding);
            holdingId = holding.getId();
        } else {
            final var h = existingHolding.get();
            holdingId = h.getId();
            if (req.getType() == TransactionType.BUY) {
                final var newAvg = pnlCalculatorService.computeNewAveragePrice(
                        h.getQuantity(), h.getAverageBuyPrice(), req.getQuantity(), req.getPrice());
                final var newQty = h.getQuantity().add(req.getQuantity());
                holdingDao.updateQuantityAndAvgPrice(holdingId, newQty, newAvg);
            } else {
                final var newQty = h.getQuantity().subtract(req.getQuantity());
                if (newQty.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("Insufficient quantity to sell");
                }
                holdingDao.updateQuantityAndAvgPrice(holdingId, newQty, h.getAverageBuyPrice());
            }
        }

        final var txn = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .holdingId(holdingId)
                .portfolioId(portfolioId)
                .type(req.getType())
                .quantity(req.getQuantity())
                .price(req.getPrice())
                .notes(req.getNotes())
                .transactedAt(req.getTransactedAt() != null ? req.getTransactedAt() : LocalDateTime.now())
                .build();
        transactionDao.insert(txn);
        return txn;
    }
}
