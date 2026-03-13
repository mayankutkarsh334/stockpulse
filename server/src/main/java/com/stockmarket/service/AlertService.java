package com.stockmarket.service;

import com.stockmarket.dao.mysql.PriceAlertDao;
import com.stockmarket.messaging.rabbitmq.RabbitMQPublisher;
import com.stockmarket.model.cache.StockQuote;
import com.stockmarket.model.dto.request.CreateAlertRequest;
import com.stockmarket.model.dto.response.AlertResponse;
import com.stockmarket.model.entity.PriceAlert;
import com.stockmarket.model.enums.AlertDirection;
import com.stockmarket.model.enums.AlertStatus;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class AlertService {

    private final PriceAlertDao priceAlertDao;
    private final QuoteService quoteService;
    private final RabbitMQPublisher rabbitMQPublisher;

    @Inject
    public AlertService(final PriceAlertDao priceAlertDao,
                        final QuoteService quoteService,
                        final RabbitMQPublisher rabbitMQPublisher) {
        this.priceAlertDao = priceAlertDao;
        this.quoteService = quoteService;
        this.rabbitMQPublisher = rabbitMQPublisher;
    }

    public PriceAlert createAlert(final CreateAlertRequest req) {
        final var alert = PriceAlert.builder()
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

    public List<AlertResponse> getAlertsByUser(final String userId, final AlertStatus status) {
        return priceAlertDao.findByUserIdAndStatus(userId, status.name())
                .stream()
                .map(this::toAlertResponse)
                .collect(Collectors.toList());
    }

    public void cancelAlert(final String alertId) {
        priceAlertDao.cancel(alertId);
    }

    public void processQuoteForAlerts(final StockQuote quote) {
        final var activeAlerts = priceAlertDao
                .findActiveBySymbolAndExchange(quote.getSymbol(), quote.getExchange().name());

        for (final PriceAlert alert : activeAlerts) {
            final var triggered = (alert.getDirection() == AlertDirection.ABOVE
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

    private AlertResponse toAlertResponse(final PriceAlert alert) {
        BigDecimal currentPrice = BigDecimal.ZERO;
        try {
            final var quote = quoteService.getCachedQuote(alert.getSymbol(), alert.getExchange());
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
