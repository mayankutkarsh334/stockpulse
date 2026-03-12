package com.stockmarket.dao.mysql;

import com.stockmarket.model.entity.Watchlist;
import com.stockmarket.model.entity.WatchlistSymbol;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import java.util.List;
import java.util.Optional;

public interface WatchlistDao {

    @SqlUpdate("INSERT INTO watchlists (id, user_id, name) VALUES (:id, :userId, :name)")
    @RegisterBeanMapper(Watchlist.class)
    void insertWatchlist(@BindBean Watchlist watchlist);

    @SqlQuery("SELECT * FROM watchlists WHERE id = :id")
    @RegisterBeanMapper(Watchlist.class)
    Optional<Watchlist> findById(@Bind("id") String id);

    @SqlQuery("SELECT * FROM watchlists WHERE user_id = :userId ORDER BY created_at DESC")
    @RegisterBeanMapper(Watchlist.class)
    List<Watchlist> findByUserId(@Bind("userId") String userId);

    @SqlUpdate("INSERT INTO watchlist_symbols (watchlist_id, symbol, exchange) VALUES (:watchlistId, :symbol, :exchange)")
    @RegisterBeanMapper(WatchlistSymbol.class)
    void insertSymbol(@BindBean WatchlistSymbol symbol);

    @SqlUpdate("DELETE FROM watchlist_symbols WHERE watchlist_id = :watchlistId AND symbol = :symbol AND exchange = :exchange")
    int removeSymbol(@Bind("watchlistId") String watchlistId,
                     @Bind("symbol") String symbol,
                     @Bind("exchange") String exchange);

    @SqlQuery("SELECT * FROM watchlist_symbols WHERE watchlist_id = :watchlistId ORDER BY added_at ASC")
    @RegisterBeanMapper(WatchlistSymbol.class)
    List<WatchlistSymbol> findSymbolsByWatchlistId(@Bind("watchlistId") String watchlistId);

    @SqlUpdate("DELETE FROM watchlists WHERE id = :id")
    int deleteById(@Bind("id") String id);
}
