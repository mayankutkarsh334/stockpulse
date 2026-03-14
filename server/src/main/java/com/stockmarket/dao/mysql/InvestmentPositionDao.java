package com.stockmarket.dao.mysql;

import com.stockmarket.model.entity.InvestmentPosition;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RegisterBeanMapper(InvestmentPosition.class)
public interface InvestmentPositionDao {

    @SqlUpdate("INSERT INTO investment_positions " +
               "(id, symbol, company_name, sector, exchange, entry_price, entry_date, quantity, " +
               "invested_amount, current_stop_loss, high_water_mark_price, picked_month, model_type, status) " +
               "VALUES (:id, :symbol, :companyName, :sector, :exchange, :entryPrice, :entryDate, :quantity, " +
               ":investedAmount, :currentStopLoss, :highWaterMarkPrice, :pickedMonth, :modelType, :status)")
    void insert(@BindBean InvestmentPosition position);

    @SqlQuery("SELECT * FROM investment_positions WHERE id = :id")
    Optional<InvestmentPosition> findById(@Bind("id") String id);

    @SqlQuery("SELECT * FROM investment_positions WHERE status = :status ORDER BY entry_date DESC")
    List<InvestmentPosition> findByStatus(@Bind("status") String status);

    @SqlQuery("SELECT * FROM investment_positions WHERE picked_month = :pickedMonth")
    Optional<InvestmentPosition> findByPickedMonth(@Bind("pickedMonth") String pickedMonth);

    @SqlQuery("SELECT * FROM investment_positions WHERE symbol = :symbol AND status = 'ACTIVE'")
    List<InvestmentPosition> findActiveBySymbol(@Bind("symbol") String symbol);

    @SqlQuery("SELECT * FROM investment_positions ORDER BY entry_date DESC")
    List<InvestmentPosition> findAll();

    @SqlUpdate("UPDATE investment_positions " +
               "SET current_stop_loss = :currentStopLoss, high_water_mark_price = :highWaterMarkPrice " +
               "WHERE id = :id")
    int updateStopLoss(@Bind("id") String id,
                       @Bind("currentStopLoss") BigDecimal currentStopLoss,
                       @Bind("highWaterMarkPrice") BigDecimal highWaterMarkPrice);

    @SqlUpdate("UPDATE investment_positions " +
               "SET status = :status, closed_at = :closedAt, close_price = :closePrice, " +
               "close_pnl = :closePnl, close_reason = :closeReason WHERE id = :id")
    int closePosition(@Bind("id") String id,
                      @Bind("status") String status,
                      @Bind("closedAt") LocalDateTime closedAt,
                      @Bind("closePrice") BigDecimal closePrice,
                      @Bind("closePnl") BigDecimal closePnl,
                      @Bind("closeReason") String closeReason);
}
