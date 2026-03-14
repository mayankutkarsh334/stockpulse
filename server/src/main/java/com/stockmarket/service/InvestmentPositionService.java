package com.stockmarket.service;

import com.stockmarket.dao.mysql.InvestmentPositionDao;
import com.stockmarket.model.dto.request.CreatePickRequest;
import com.stockmarket.model.dto.response.PositionResponse;
import com.stockmarket.model.entity.InvestmentPosition;
import com.stockmarket.model.enums.CloseReason;
import com.stockmarket.model.enums.PositionStatus;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class InvestmentPositionService {

    private static final BigDecimal INVESTED_AMOUNT   = BigDecimal.valueOf(20_000);
    private static final BigDecimal INITIAL_STOP_MULT = BigDecimal.valueOf(0.85);
    private static final int        EXPIRY_YEARS      = 2;

    private final InvestmentPositionDao dao;

    @Inject
    public InvestmentPositionService(final InvestmentPositionDao dao) {
        this.dao = dao;
    }

    /** Creates this month's pick. Throws 409 if a pick already exists for the current month. */
    public InvestmentPosition createMonthlyPick(final CreatePickRequest req) {
        final String pickedMonth = currentMonth();

        if (dao.findByPickedMonth(pickedMonth).isPresent()) {
            throw new WebApplicationException(
                    Response.status(409)
                            .entity(Map.of("error", "A pick already exists for " + pickedMonth))
                            .build());
        }

        final BigDecimal entryPrice  = req.getEntryPrice();
        final BigDecimal quantity    = INVESTED_AMOUNT.divide(entryPrice, 6, RoundingMode.HALF_UP);
        final BigDecimal initialStop = entryPrice.multiply(INITIAL_STOP_MULT).setScale(4, RoundingMode.HALF_UP);

        final InvestmentPosition position = InvestmentPosition.builder()
                .id(UUID.randomUUID().toString())
                .symbol(req.getSymbol())
                .companyName(req.getCompanyName())
                .sector(req.getSector())
                .exchange(req.getExchange())
                .entryPrice(entryPrice)
                .entryDate(LocalDate.now())
                .quantity(quantity)
                .investedAmount(INVESTED_AMOUNT)
                .currentStopLoss(initialStop)
                .highWaterMarkPrice(entryPrice)
                .pickedMonth(pickedMonth)
                .modelType(req.getModelType())
                .status(PositionStatus.ACTIVE)
                .build();

        dao.insert(position);
        log.info("Created monthly pick {} for {}", req.getSymbol(), pickedMonth);
        return position;
    }

    /**
     * Updates trailing stop for a live price tick.
     * @return true if the position was closed (stop hit), false if still active
     */
    public boolean updateStopLoss(final String positionId, final BigDecimal currentPrice) {
        final Optional<InvestmentPosition> opt = dao.findById(positionId);
        if (opt.isEmpty() || opt.get().getStatus() != PositionStatus.ACTIVE) return false;

        final InvestmentPosition pos = opt.get();

        // Stop hit → close immediately
        if (currentPrice.compareTo(pos.getCurrentStopLoss()) <= 0) {
            closePosition(positionId, currentPrice, CloseReason.STOP_LOSS_HIT, PositionStatus.STOPPED_OUT);
            log.info("Stop hit for {} at {}", pos.getSymbol(), currentPrice);
            return true;
        }

        // Compute gain and new stop level
        final double gain = currentPrice.subtract(pos.getEntryPrice())
                .divide(pos.getEntryPrice(), 8, RoundingMode.HALF_UP)
                .doubleValue();
        final BigDecimal newStop  = computeStopLevel(pos.getEntryPrice(), gain);
        final BigDecimal updStop  = newStop.max(pos.getCurrentStopLoss());  // never moves down
        final BigDecimal updHwm   = currentPrice.max(pos.getHighWaterMarkPrice());

        dao.updateStopLoss(positionId, updStop, updHwm);
        return false;
    }

    /** Force-closes any ACTIVE positions that have been open for ≥ 2 years without hitting +30%. */
    public void checkExpiryRules() {
        final LocalDate cutoff = LocalDate.now().minusYears(EXPIRY_YEARS);
        dao.findByStatus(PositionStatus.ACTIVE.name()).stream()
                .filter(p -> p.getEntryDate().isBefore(cutoff))
                .forEach(p -> {
                    log.info("Force-closing expired position {} ({})", p.getId(), p.getSymbol());
                    closePosition(p.getId(), p.getHighWaterMarkPrice(),
                            CloseReason.TARGET_MISSED, PositionStatus.TARGET_MISSED);
                });
    }

    public void closePosition(final String id, final BigDecimal closePrice,
                               final CloseReason reason, final PositionStatus newStatus) {
        final Optional<InvestmentPosition> opt = dao.findById(id);
        if (opt.isEmpty()) return;
        final InvestmentPosition pos = opt.get();
        final BigDecimal pnl = closePrice.subtract(pos.getEntryPrice())
                .multiply(pos.getQuantity())
                .setScale(4, RoundingMode.HALF_UP);
        dao.closePosition(id, newStatus.name(), LocalDateTime.now(), closePrice, pnl, reason.name());
    }

    public List<PositionResponse> listPositions(final String status) {
        final List<InvestmentPosition> positions = (status == null || status.isBlank())
                ? dao.findAll()
                : dao.findByStatus(status.toUpperCase());
        return positions.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Optional<PositionResponse> getCurrentMonthPosition() {
        return dao.findByPickedMonth(currentMonth()).map(this::toResponse);
    }

    public Optional<PositionResponse> getPositionById(final String id) {
        return dao.findById(id).map(this::toResponse);
    }

    public List<InvestmentPosition> getActivePositionsBySymbol(final String symbol) {
        return dao.findActiveBySymbol(symbol);
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private static String currentMonth() {
        return LocalDate.now().toString().substring(0, 7); // YYYY-MM
    }

    /**
     * Trailing stop formula:
     *   gain < 30%  → entry × 0.85  (initial −15% stop)
     *   tier 0 (≥30%)  → entry × 1.25  (protect 25%)
     *   tier 1 (≥40%)  → entry × 1.30  (protect 30%)
     *   tier 2 (≥50%)  → entry × 1.35  (protect 35%) …
     */
    static BigDecimal computeStopLevel(final BigDecimal entryPrice, final double gain) {
        if (gain < 0.30) {
            return entryPrice.multiply(BigDecimal.valueOf(0.85)).setScale(4, RoundingMode.HALF_UP);
        }
        final int tier = (int) Math.floor((gain - 0.30) / 0.10);
        final double mult = 1.0 + 0.25 + tier * 0.05;
        return entryPrice.multiply(BigDecimal.valueOf(mult)).setScale(4, RoundingMode.HALF_UP);
    }

    private PositionResponse toResponse(final InvestmentPosition p) {
        final double gainPct;
        if (p.getStatus() == PositionStatus.ACTIVE) {
            gainPct = p.getHighWaterMarkPrice().subtract(p.getEntryPrice())
                    .divide(p.getEntryPrice(), 6, RoundingMode.HALF_UP)
                    .doubleValue() * 100;
        } else if (p.getClosePnl() != null && p.getInvestedAmount() != null
                && p.getInvestedAmount().compareTo(BigDecimal.ZERO) != 0) {
            gainPct = p.getClosePnl()
                    .divide(p.getInvestedAmount(), 6, RoundingMode.HALF_UP)
                    .doubleValue() * 100;
        } else {
            gainPct = 0.0;
        }

        return PositionResponse.builder()
                .id(p.getId())
                .symbol(p.getSymbol())
                .companyName(p.getCompanyName())
                .sector(p.getSector())
                .exchange(p.getExchange())
                .entryPrice(p.getEntryPrice())
                .entryDate(p.getEntryDate())
                .quantity(p.getQuantity())
                .investedAmount(p.getInvestedAmount())
                .currentStopLoss(p.getCurrentStopLoss())
                .highWaterMarkPrice(p.getHighWaterMarkPrice())
                .pickedMonth(p.getPickedMonth())
                .modelType(p.getModelType())
                .status(p.getStatus())
                .closedAt(p.getClosedAt())
                .closePrice(p.getClosePrice())
                .closePnl(p.getClosePnl())
                .closeReason(p.getCloseReason())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .currentGainPct(gainPct)
                .build();
    }
}
