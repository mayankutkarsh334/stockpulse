package com.stockmarket.dao.mysql;

import com.stockmarket.model.entity.Portfolio;
import com.stockmarket.model.enums.Currency;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import java.util.List;
import java.util.Optional;

@RegisterBeanMapper(Portfolio.class)
public interface PortfolioDao {

    @SqlUpdate("INSERT INTO portfolios (id, user_id, name, currency, type) VALUES (:id, :userId, :name, :currency, :type)")
    void insert(@BindBean Portfolio portfolio);

    @SqlQuery("SELECT * FROM portfolios WHERE id = :id")
    Optional<Portfolio> findById(@Bind("id") String id);

    @SqlQuery("SELECT * FROM portfolios WHERE user_id = :userId ORDER BY created_at DESC")
    List<Portfolio> findByUserId(@Bind("userId") String userId);

    @SqlQuery("SELECT * FROM portfolios WHERE user_id = :userId AND type = :type ORDER BY created_at DESC")
    List<Portfolio> findByUserIdAndType(@Bind("userId") String userId, @Bind("type") String type);

    @SqlUpdate("UPDATE portfolios SET name = :name WHERE id = :id")
    int updateName(@Bind("id") String id, @Bind("name") String name);

    @SqlUpdate("DELETE FROM portfolios WHERE id = :id")
    int deleteById(@Bind("id") String id);
}
