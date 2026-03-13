package com.stockmarket.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.stockmarket.dao.aerospike.FundamentalsCache;
import com.stockmarket.dao.aerospike.QuoteCache;
import com.stockmarket.dao.aerospike.TechnicalsCache;

public class CacheModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(QuoteCache.class).in(Singleton.class);
        bind(FundamentalsCache.class).in(Singleton.class);
        bind(TechnicalsCache.class).in(Singleton.class);
    }
}
