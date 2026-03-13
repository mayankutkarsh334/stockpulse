package com.stockmarket.inject;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.stockmarket.kafka.producer.AnalysisJobProducer;
import com.stockmarket.kafka.producer.PriceAlertProducer;

public class KafkaModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(PriceAlertProducer.class).in(Singleton.class);
        bind(AnalysisJobProducer.class).in(Singleton.class);
    }
}
