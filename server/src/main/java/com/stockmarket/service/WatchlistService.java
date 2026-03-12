package com.stockmarket.service;

import com.stockmarket.dao.mysql.WatchlistDao;
import com.stockmarket.model.dto.request.CreateWatchlistRequest;
import com.stockmarket.model.dto.response.WatchlistResponse;
import com.stockmarket.model.entity.Watchlist;
import com.stockmarket.model.entity.WatchlistSymbol;
import com.stockmarket.model.enums.Exchange;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistDao watchlistDao;
    private final QuoteService quoteService;

    public Watchlist createWatchlist(CreateWatchlistRequest req) {
        Watchlist watchlist = Watchlist.builder()
                .id(UUID.randomUUID().toString())
                .userId(req.getUserId())
                .name(req.getName())
                .build();
        watchlistDao.insertWatchlist(watchlist);
        return watchlist;
    }

    public WatchlistResponse getWatchlistWithPrices(String watchlistId) {
        Watchlist wl = watchlistDao.findById(watchlistId)
                .orElseThrow(() -> new NotFoundException("Watchlist not found: " + watchlistId));
        List<WatchlistSymbol> symbols = watchlistDao.findSymbolsByWatchlistId(watchlistId);

        List<WatchlistResponse.WatchlistItemResponse> items = symbols.stream().map(s -> {
            BigDecimal price = BigDecimal.ZERO;
            BigDecimal change = BigDecimal.ZERO;
            BigDecimal changePercent = BigDecimal.ZERO;
            try {
                var quote = quoteService.getQuote(s.getSymbol(), s.getExchange());
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

    public void addSymbol(String watchlistId, String symbol, Exchange exchange) {
        watchlistDao.findById(watchlistId)
                .orElseThrow(() -> new NotFoundException("Watchlist not found: " + watchlistId));
        WatchlistSymbol ws = WatchlistSymbol.builder()
                .watchlistId(watchlistId)
                .symbol(symbol.toUpperCase())
                .exchange(exchange)
                .addedAt(LocalDateTime.now())
                .build();
        watchlistDao.insertSymbol(ws);
    }

    public void removeSymbol(String watchlistId, String symbol, Exchange exchange) {
        watchlistDao.removeSymbol(watchlistId, symbol, exchange.name());
    }
}
