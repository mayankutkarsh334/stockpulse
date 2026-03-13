package com.stockmarket.service;

import com.stockmarket.dao.mysql.WatchlistDao;
import com.stockmarket.model.dto.request.CreateWatchlistRequest;
import com.stockmarket.model.dto.response.WatchlistResponse;
import com.stockmarket.model.entity.Watchlist;
import com.stockmarket.model.entity.WatchlistSymbol;
import com.stockmarket.model.enums.Exchange;
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
public class WatchlistService {

    private final WatchlistDao watchlistDao;
    private final QuoteService quoteService;

    @Inject
    public WatchlistService(final WatchlistDao watchlistDao, final QuoteService quoteService) {
        this.watchlistDao = watchlistDao;
        this.quoteService = quoteService;
    }

    public Watchlist createWatchlist(final CreateWatchlistRequest req) {
        final var watchlist = Watchlist.builder()
                .id(UUID.randomUUID().toString())
                .userId(req.getUserId())
                .name(req.getName())
                .build();
        watchlistDao.insertWatchlist(watchlist);
        return watchlist;
    }

    public WatchlistResponse getWatchlistWithPrices(final String watchlistId) {
        final var wl = watchlistDao.findById(watchlistId)
                .orElseThrow(() -> new NotFoundException("Watchlist not found: " + watchlistId));
        final var symbols = watchlistDao.findSymbolsByWatchlistId(watchlistId);

        final var items = symbols.stream().map(s -> {
            BigDecimal price = BigDecimal.ZERO;
            BigDecimal change = BigDecimal.ZERO;
            BigDecimal changePercent = BigDecimal.ZERO;
            try {
                final var quote = quoteService.getQuote(s.getSymbol(), s.getExchange());
                price = quote.getPrice();
                change = quote.getChange();
                changePercent = quote.getChangePercent();
            } catch (Exception e) {
                log.warn("Could not fetch quote for {}/{}", s.getSymbol(), s.getExchange());
            }
            return WatchlistResponse.WatchlistItemResponse.builder()
                    .symbol(s.getSymbol())
                    .exchange(s.getExchange())
                    .currentPrice(price)
                    .change(change)
                    .changePercent(changePercent)
                    .build();
        }).collect(Collectors.toList());

        return WatchlistResponse.builder()
                .id(wl.getId())
                .userId(wl.getUserId())
                .name(wl.getName())
                .symbols(items)
                .createdAt(wl.getCreatedAt())
                .build();
    }

    public void addSymbol(final String watchlistId, final String symbol, final Exchange exchange) {
        watchlistDao.findById(watchlistId)
                .orElseThrow(() -> new NotFoundException("Watchlist not found: " + watchlistId));
        final var ws = WatchlistSymbol.builder()
                .watchlistId(watchlistId)
                .symbol(symbol.toUpperCase())
                .exchange(exchange)
                .addedAt(LocalDateTime.now())
                .build();
        watchlistDao.insertSymbol(ws);
    }

    public void removeSymbol(final String watchlistId, final String symbol, final Exchange exchange) {
        watchlistDao.removeSymbol(watchlistId, symbol, exchange.name());
    }
}
