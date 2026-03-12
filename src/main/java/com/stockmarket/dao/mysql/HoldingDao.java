package com.stockmarket.dao.mysql;

import com.stockmarket.model.entity.Holding;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RegisterBeanMapper(Holding.class)
public interface HoldingDao {

    @SqlUpdate("INSERT INTO holdings (id, portfolio_id, symbol, exchange, quantity, average_buy_price, currency) " +
               "VALUES (:id, :portfolioId, :symbol, :exchange, :quantity, :averageBuyPrice, :currency)")
    void insert(@BindBean Holding holding);

    @SqlQuery("SELECT * FROM holdings WHERE portfolio_id = :portfolioId")
    List<Holding> findByPortfolioId(@Bind("portfolioId") String portfolioId);

    @SqlQuery("SELECT * FROM holdings WHERE portfolio_id = :portfolioId AND symbol = :symbol AND exchange = :exchange")
    Optional<Holding> findByPortfolioSymbolExchange(
            @Bind("portfolioId") String portfolioId,
            @Bind("symbol") String symbol,
            @Bind("exchange") String exchange);

    @SqlQuery("SELECT * FROM holdings WHERE id = :id")
    Optional<Holding> findById(@Bind("id") String id);

    @SqlUpdate("UPDATE holdings SET quantity = :quantity, average_buy_price = :avgPrice WHERE id = :id")
    int updateQuantityAndAvgPrice(@Bind("id") String id,
                                  @Bind("quantity") BigDecimal quantity,
                                  @Bind("avgPrice") BigDecimal avgPrice);

    @SqlUpdate("DELETE FROM holdings WHERE id = :id")
    int deleteById(@Bind("id") String id);
}
