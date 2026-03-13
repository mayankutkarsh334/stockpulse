package com.stockmarket.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.stockmarket.MarketServiceConfiguration;
import com.stockmarket.dao.mysql.AnalysisConfigDao;
import com.stockmarket.dao.mysql.HoldingDao;
import com.stockmarket.dao.mysql.PortfolioDao;
import com.stockmarket.dao.mysql.PriceAlertDao;
import com.stockmarket.dao.mysql.TransactionDao;
import com.stockmarket.dao.mysql.WatchlistDao;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jdbi3.JdbiFactory;
import org.jdbi.v3.core.Jdbi;

public class DaoModule extends AbstractModule {

    @Override
    protected void configure() {}

    @Provides
    @Singleton
    Jdbi provideJdbi(final MarketServiceConfiguration config, final Environment environment) {
        final var factory = new JdbiFactory();
        return factory.build(environment, config.getDatabase(), "mysql");
    }

    @Provides
    @Singleton
    PortfolioDao providePortfolioDao(final Jdbi jdbi) {
        return jdbi.onDemand(PortfolioDao.class);
    }

    @Provides
    @Singleton
    HoldingDao provideHoldingDao(final Jdbi jdbi) {
        return jdbi.onDemand(HoldingDao.class);
    }

    @Provides
    @Singleton
    TransactionDao provideTransactionDao(final Jdbi jdbi) {
        return jdbi.onDemand(TransactionDao.class);
    }

    @Provides
    @Singleton
    WatchlistDao provideWatchlistDao(final Jdbi jdbi) {
        return jdbi.onDemand(WatchlistDao.class);
    }

    @Provides
    @Singleton
    PriceAlertDao providePriceAlertDao(final Jdbi jdbi) {
        return jdbi.onDemand(PriceAlertDao.class);
    }

    @Provides
    @Singleton
    AnalysisConfigDao provideAnalysisConfigDao(final Jdbi jdbi) {
        return jdbi.onDemand(AnalysisConfigDao.class);
    }
}
