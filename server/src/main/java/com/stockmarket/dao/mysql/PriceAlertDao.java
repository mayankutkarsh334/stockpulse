package com.stockmarket.dao.mysql;

import com.stockmarket.model.entity.PriceAlert;
import com.stockmarket.model.enums.AlertStatus;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RegisterBeanMapper(PriceAlert.class)
public interface PriceAlertDao {

    @SqlUpdate("INSERT INTO price_alerts (id, user_id, symbol, exchange, target_price, direction, status) " +
               "VALUES (:id, :userId, :symbol, :exchange, :targetPrice, :direction, :status)")
    void insert(@BindBean PriceAlert alert);

    @SqlQuery("SELECT * FROM price_alerts WHERE id = :id")
    Optional<PriceAlert> findById(@Bind("id") String id);

    @SqlQuery("SELECT * FROM price_alerts WHERE user_id = :userId AND status = :status ORDER BY created_at DESC")
    List<PriceAlert> findByUserIdAndStatus(@Bind("userId") String userId, @Bind("status") String status);

    @SqlQuery("SELECT * FROM price_alerts WHERE symbol = :symbol AND exchange = :exchange AND status = 'ACTIVE'")
    List<PriceAlert> findActiveBySymbolAndExchange(@Bind("symbol") String symbol, @Bind("exchange") String exchange);

    @SqlUpdate("UPDATE price_alerts SET status = :status, triggered_at = :triggeredAt WHERE id = :id")
    int updateStatus(@Bind("id") String id, @Bind("status") String status, @Bind("triggeredAt") LocalDateTime triggeredAt);

    @SqlUpdate("UPDATE price_alerts SET status = 'CANCELLED' WHERE id = :id")
    int cancel(@Bind("id") String id);
}
