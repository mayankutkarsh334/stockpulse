package com.stockmarket;

import com.stockmarket.config.AerospikeConfig;
import com.stockmarket.dao.aerospike.FundamentalsCache;
import com.stockmarket.dao.aerospike.QuoteCache;
import com.stockmarket.dao.aerospike.TechnicalsCache;
import com.stockmarket.dao.mysql.*;
import com.stockmarket.health.*;
import com.stockmarket.kafka.consumer.AnalysisJobConsumer;
import com.stockmarket.kafka.consumer.PriceAlertConsumer;
import com.stockmarket.kafka.producer.AnalysisJobProducer;
import com.stockmarket.kafka.producer.PriceAlertProducer;
import com.stockmarket.messaging.rabbitmq.RabbitMQPublisher;
import com.stockmarket.resource.*;
import com.stockmarket.service.*;
import com.aerospike.client.AerospikeClient;
import com.aerospike.client.policy.ClientPolicy;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.db.DataSourceFactory;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.jdbi.v3.core.Jdbi;
import java.util.Set;

public class MarketServiceApplication extends Application<MarketServiceConfiguration> {

    public static void main(String[] args) throws Exception {
        new MarketServiceApplication().run(args);
    }

    @Override
    public String getName() {
        return "stock-investing-service";
    }

    @Override
    public void initialize(Bootstrap<MarketServiceConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/assets/", "/ui", "index.html", "assets"));
        bootstrap.addBundle(new AssetsBundle("/swagger-assets/", "/swagger", "index.html", "swagger"));
        bootstrap.addBundle(new MigrationsBundle<>() {
            @Override
            public DataSourceFactory getDataSourceFactory(MarketServiceConfiguration configuration) {
                return configuration.getDatabase();
            }
        });
    }

    @Override
    public void run(MarketServiceConfiguration config, Environment environment) throws Exception {
        // JDBI setup
        final JdbiFactory jdbiFactory = new JdbiFactory();
        final Jdbi jdbi = jdbiFactory.build(environment, config.getDatabase(), "mysql");

        // DAOs
        PortfolioDao portfolioDao = jdbi.onDemand(PortfolioDao.class);
        HoldingDao holdingDao = jdbi.onDemand(HoldingDao.class);
        TransactionDao transactionDao = jdbi.onDemand(TransactionDao.class);
        WatchlistDao watchlistDao = jdbi.onDemand(WatchlistDao.class);
        PriceAlertDao priceAlertDao = jdbi.onDemand(PriceAlertDao.class);
        AnalysisConfigDao analysisConfigDao = jdbi.onDemand(AnalysisConfigDao.class);

        // Aerospike
        AerospikeConfig aeroCfg = config.getAerospike();
        ClientPolicy clientPolicy = new ClientPolicy();
        AerospikeClient aerospikeClient = new AerospikeClient(clientPolicy,
                aeroCfg.getHosts()[0].getHost(), aeroCfg.getHosts()[0].getPort());
        environment.lifecycle().manage(new io.dropwizard.lifecycle.Managed() {
            @Override public void start() {}
            @Override public void stop() { aerospikeClient.close(); }
        });

        // Cache layers
        QuoteCache quoteCache = new QuoteCache(aerospikeClient, aeroCfg);
        FundamentalsCache fundamentalsCache = new FundamentalsCache(aerospikeClient, aeroCfg);
        TechnicalsCache technicalsCache = new TechnicalsCache(aerospikeClient, aeroCfg);

        // Alpha Vantage client
        com.stockmarket.client.AlphaVantageClient avClient =
                new com.stockmarket.client.AlphaVantageClient(config.getAlphaVantage());

        // Kafka producers
        PriceAlertProducer priceAlertProducer = new PriceAlertProducer(config.getKafka());
        AnalysisJobProducer analysisJobProducer = new AnalysisJobProducer(config.getKafka());

        // RabbitMQ
        RabbitMQPublisher rabbitMQPublisher = new RabbitMQPublisher(config.getRabbitMQ());
        environment.lifecycle().manage(rabbitMQPublisher);

        // Services
        QuoteService quoteService = new QuoteService(avClient, quoteCache, priceAlertProducer);
        PnLCalculatorService pnlCalculatorService = new PnLCalculatorService(quoteService);
        PortfolioService portfolioService = new PortfolioService(portfolioDao, holdingDao,
                transactionDao, pnlCalculatorService);
        WatchlistService watchlistService = new WatchlistService(watchlistDao, quoteService);
        AlertService alertService = new AlertService(priceAlertDao, quoteService, rabbitMQPublisher);
        ScreenerService screenerService = new ScreenerService(fundamentalsCache, technicalsCache,
                avClient, analysisJobProducer);
        AnalysisService analysisService = new AnalysisService(fundamentalsCache, technicalsCache,
                avClient, analysisConfigDao, analysisJobProducer);

        // Resources
        environment.jersey().register(new PortfolioResource(portfolioService));
        environment.jersey().register(new WatchlistResource(watchlistService));
        environment.jersey().register(new AlertResource(alertService));
        environment.jersey().register(new ScreenerResource(screenerService));
        environment.jersey().register(new AnalysisResource(analysisService));

        // OpenAPI / Swagger
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("Stock Investing Service")
                        .version("1.0.0")
                        .description("Portfolio management, stock screening, and multi-model analysis engine"));
        SwaggerConfiguration oasConfig = new SwaggerConfiguration()
                .openAPI(openAPI)
                .prettyPrint(true)
                .resourcePackages(Set.of("com.stockmarket.resource"));
        environment.jersey().register(new OpenApiResource().openApiConfiguration(oasConfig));

        // Health checks (mysql is auto-registered by JdbiFactory)
        environment.healthChecks().register("aerospike", new AerospikeHealthCheck(aerospikeClient));
        environment.healthChecks().register("kafka", new KafkaHealthCheck(config.getKafka()));
        environment.healthChecks().register("alphavantage", new AlphaVantageHealthCheck(avClient));

        // Kafka consumers (start as background threads)
        PriceAlertConsumer priceAlertConsumer = new PriceAlertConsumer(config.getKafka(), alertService);
        AnalysisJobConsumer analysisJobConsumer = new AnalysisJobConsumer(config.getKafka(), analysisService);
        environment.lifecycle().manage(priceAlertConsumer);
        environment.lifecycle().manage(analysisJobConsumer);
    }
}
