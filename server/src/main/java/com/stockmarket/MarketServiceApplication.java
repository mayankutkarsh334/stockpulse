package com.stockmarket;

import com.stockmarket.inject.CacheModule;
import com.stockmarket.inject.DaoModule;
import com.stockmarket.inject.InfrastructureModule;
import com.stockmarket.inject.KafkaModule;
import com.stockmarket.inject.ServiceModule;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import ru.vyarus.dropwizard.guice.GuiceBundle;

import java.util.Set;

public class MarketServiceApplication extends Application<MarketServiceConfiguration> {

    public static void main(final String[] args) throws Exception {
        new MarketServiceApplication().run(args);
    }

    @Override
    public String getName() {
        return "stock-investing-service";
    }

    @Override
    public void initialize(final Bootstrap<MarketServiceConfiguration> bootstrap) {

        bootstrap.addBundle(GuiceBundle.builder()
                .enableAutoConfig("com.stockmarket")
                .modules(
                        new InfrastructureModule(),
                        new CacheModule(),
                        new DaoModule(),
                        new KafkaModule(),
                        new ServiceModule()
                )
                .build());
        bootstrap.addBundle(new AssetsBundle("/assets/", "/ui", "index.html", "assets"));
        bootstrap.addBundle(new AssetsBundle("/swagger-assets/", "/swagger", "index.html", "swagger"));
        bootstrap.addBundle(new MigrationsBundle<>() {
            @Override
            public DataSourceFactory getDataSourceFactory(final MarketServiceConfiguration configuration) {
                return configuration.getDatabase();
            }
        });
    }

    @Override
    public void run(final MarketServiceConfiguration config, final Environment environment) throws Exception {

        final var openAPI = new OpenAPI()
                .info(new Info()
                        .title("Stock Investing Service")
                        .version("1.0.0")
                        .description("Portfolio management, stock screening, and multi-model analysis engine"));

        final var oasConfig = new SwaggerConfiguration()
                .openAPI(openAPI)
                .prettyPrint(true)
                .resourcePackages(Set.of("com.stockmarket.resource"));

        environment.jersey().register(new OpenApiResource().openApiConfiguration(oasConfig));
    }
}
