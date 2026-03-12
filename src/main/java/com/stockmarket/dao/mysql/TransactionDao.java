package com.stockmarket.dao.mysql;

import com.stockmarket.model.entity.Transaction;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import java.util.List;

@RegisterBeanMapper(Transaction.class)
public interface TransactionDao {

    @SqlUpdate("INSERT INTO transactions (id, holding_id, portfolio_id, type, quantity, price, notes, transacted_at) " +
               "VALUES (:id, :holdingId, :portfolioId, :type, :quantity, :price, :notes, :transactedAt)")
    void insert(@BindBean Transaction transaction);

    @SqlQuery("SELECT * FROM transactions WHERE portfolio_id = :portfolioId ORDER BY transacted_at DESC")
    List<Transaction> findByPortfolioId(@Bind("portfolioId") String portfolioId);

    @SqlQuery("SELECT * FROM transactions WHERE holding_id = :holdingId ORDER BY transacted_at DESC")
    List<Transaction> findByHoldingId(@Bind("holdingId") String holdingId);

    @SqlQuery("SELECT * FROM transactions WHERE portfolio_id = :portfolioId " +
              "AND transacted_at >= :fromDate ORDER BY transacted_at ASC")
    List<Transaction> findByPortfolioIdFrom(@Bind("portfolioId") String portfolioId,
                                             @Bind("fromDate") java.time.LocalDateTime fromDate);
}
