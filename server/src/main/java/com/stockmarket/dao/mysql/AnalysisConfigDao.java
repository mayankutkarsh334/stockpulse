package com.stockmarket.dao.mysql;

import com.stockmarket.model.entity.AnalysisConfig;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import java.util.List;
import java.util.Optional;

@RegisterBeanMapper(AnalysisConfig.class)
public interface AnalysisConfigDao {

    @SqlUpdate("INSERT INTO analysis_configs (id, user_id, name, model_type, params_json) " +
               "VALUES (:id, :userId, :name, :modelType, :paramsJson)")
    void insert(@BindBean AnalysisConfig config);

    @SqlQuery("SELECT * FROM analysis_configs WHERE id = :id")
    Optional<AnalysisConfig> findById(@Bind("id") String id);

    @SqlQuery("SELECT * FROM analysis_configs WHERE user_id = :userId ORDER BY created_at DESC")
    List<AnalysisConfig> findByUserId(@Bind("userId") String userId);

    @SqlUpdate("DELETE FROM analysis_configs WHERE id = :id")
    int deleteById(@Bind("id") String id);
}
