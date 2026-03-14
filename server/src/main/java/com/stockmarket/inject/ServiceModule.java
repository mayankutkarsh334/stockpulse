package com.stockmarket.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.stockmarket.service.AlertService;
import com.stockmarket.service.AnalysisService;
import com.stockmarket.service.CsvAnalysisService;
import com.stockmarket.service.InvestmentPositionService;
import com.stockmarket.service.PnLCalculatorService;
import com.stockmarket.service.PortfolioService;
import com.stockmarket.service.QuoteService;
import com.stockmarket.service.ScreenerService;
import com.stockmarket.service.WatchlistService;

public class ServiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(QuoteService.class).in(Singleton.class);
        bind(PnLCalculatorService.class).in(Singleton.class);
        bind(PortfolioService.class).in(Singleton.class);
        bind(WatchlistService.class).in(Singleton.class);
        bind(AlertService.class).in(Singleton.class);
        bind(ScreenerService.class).in(Singleton.class);
        bind(AnalysisService.class).in(Singleton.class);
        bind(CsvAnalysisService.class).in(Singleton.class);
        bind(InvestmentPositionService.class).in(Singleton.class);
    }
}
