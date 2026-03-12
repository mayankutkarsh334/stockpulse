package com.stockmarket.service;

import com.stockmarket.dao.mysql.PriceAlertDao;
import com.stockmarket.messaging.rabbitmq.RabbitMQPublisher;
import com.stockmarket.model.cache.StockQuote;
import com.stockmarket.model.dto.request.CreateAlertRequest;
import com.stockmarket.model.dto.response.AlertResponse;
import com.stockmarket.model.entity.PriceAlert;
import com.stockmarket.model.enums.AlertDirection;
import com.stockmarket.model.enums.AlertStatus;
import com.stockmarket.model.enums.Exchange;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class AlertService {

    private final PriceAlertDao priceAlertDao;
    private final QuoteService quoteService;
    private final RabbitMQPublisher rabbitMQPublisher;

    public PriceAlert createAlert(CreateAlertRequest req) {
        PriceAlert alert = PriceAlert.builder()
                .id(UUID.randomUUID().toString())
                .userId(req.getUserId())
                .symbol(req.getSymbol().toUpperCase())
                .exchange(req.getExchange())
                .targetPrice(req.getTargetPrice())
                .direction(req.getDirection())
                .status(AlertStatus.ACTIVE)
                .build();
        priceAlertDao.insert(alert);
        return alert;
    }

    public List<AlertResponse> getAlertsByUser(String userId, AlertStatus status) {
        return priceAlertDao.findByUserIdAndStatus(userId, status.name())
                .stream()
                .map(this::toAlertResponse)
                .collect(Collectors.toList());
    }

    public void cancelAlert(String alertId) {
        priceAlertDao.cancel(alertId);
    }

    public void processQuoteForAlerts(StockQuote quote) {
        List<PriceAlert> activeAlerts = priceAlertDao
                .findActiveBySymbolAndExchange(quote.getSymbol(), quote.getExchange().name());

        for (PriceAlert alert : activeAlerts) {
            boolean triggered = (alert.getDirection() == AlertDirection.ABOVE
                    && quote.getPrice().compareTo(alert.getTargetPrice()) >= 0)
                    || (alert.getDirection() == AlertDirection.BELOW
                    && quote.getPrice().compareTo(alert.getTargetPrice()) <= 0);

            if (triggered) {
                log.info("Alert triggered: {} {} {} {}", alert.getSymbol(), alert.getDirection(),
                        alert.getTargetPrice(), quote.getPrice());
                priceAlertDao.updateStatus(alert.getId(), AlertStatus.TRIGGERED.name(), LocalDateTime.now());
                try {
                    rabbitMQPublisher.publishAlertNotification(alert, quote.getPrice());
                } catch (Exception e) {
                    log.error("Failed to publish alert notification: {}", e.getMessage());
                }
            }
        }
    }

    private AlertResponse toAlertResponse(PriceAlert alert) {
        BigDecimal currentPrice = BigDecimal.ZERO;
        try {
            var quote = quoteService.getCachedQuote(alert.getSymbol(), alert.getExchange());
            if (quote.isPresent()) currentPrice = quote.get().getPrice();
        } catch (Exception ignored) {}

        return AlertResponse.builder()
                .id(alert.getId())
                .userId(alert.getUserId())
                .symbol(alert.getSymbol())
                .exchange(alert.getExchange())
                .targetPrice(alert.getTargetPrice())
                .currentPrice(currentPrice)
                .direction(alert.getDirection())
                .status(alert.getStatus())
                .createdAt(alert.getCreatedAt())
                .triggeredAt(alert.getTriggeredAt())
                .build();
    }
}
