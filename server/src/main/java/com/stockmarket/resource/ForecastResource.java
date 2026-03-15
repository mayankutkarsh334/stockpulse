package com.stockmarket.resource;

import com.stockmarket.model.dto.response.SimulationResult;
import com.stockmarket.service.ForecastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Singleton
@Path("/forecast")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Forecast")
public class ForecastResource {

    private final ForecastService forecastService;

    @Inject
    public ForecastResource(final ForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @GET
    @Path("/price/{symbol}")
    @Operation(summary = "Get latest closing price for a stock symbol")
    public Response getPrice(
            @PathParam("symbol") final String symbol,
            @QueryParam("exchange") @DefaultValue("NSE") final String exchange) {

        return forecastService.getPrice(symbol, exchange)
                .<Response>map(price -> Response.ok(Map.of(
                        "symbol", symbol.toUpperCase(),
                        "exchange", exchange,
                        "price", price)).build())
                .orElse(Response.status(503)
                        .entity(Map.of("error", "ml-service unavailable"))
                        .build());
    }

    @GET
    @Path("/{symbol}")
    @Operation(summary = "Run Monte Carlo + ARIMA + GARCH forecast for a stock")
    public Response getForecast(
            @PathParam("symbol") final String symbol,
            @QueryParam("exchange") @DefaultValue("NSE") final String exchange,
            @QueryParam("entryPrice") final Double entryPrice,
            @QueryParam("stopLoss") final Double stopLoss,
            @QueryParam("targetPct") @DefaultValue("30.0") final double targetPct) {

        if (entryPrice == null) {
            return Response.status(422)
                    .entity(Map.of("error", "entryPrice required"))
                    .build();
        }
        if (stopLoss == null) {
            return Response.status(422)
                    .entity(Map.of("error", "stopLoss required"))
                    .build();
        }

        return forecastService.forecast(symbol, exchange, entryPrice, stopLoss, targetPct)
                .<Response>map(result -> Response.ok(result).build())
                .orElse(Response.status(503)
                        .entity(Map.of("error", "ml-service unavailable"))
                        .build());
    }

    @POST
    @Path("/{symbol}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Run forecast with optional extra CSV indicators")
    public Response postForecast(
            @PathParam("symbol") final String symbol,
            final ForecastRequestBody body) {

        if (body.getEntryPrice() == null) {
            return Response.status(422)
                    .entity(Map.of("error", "entryPrice required"))
                    .build();
        }
        if (body.getStopLoss() == null) {
            return Response.status(422)
                    .entity(Map.of("error", "stopLoss required"))
                    .build();
        }

        final double targetPct = body.getTargetPct() != null ? body.getTargetPct() : 30.0;

        return forecastService.forecastWithIndicators(
                symbol,
                body.getExchange() != null ? body.getExchange() : "NSE",
                body.getEntryPrice(),
                body.getStopLoss(),
                targetPct,
                body.getExtraIndicators())
                .<Response>map(result -> Response.ok(result).build())
                .orElse(Response.status(503)
                        .entity(Map.of("error", "ml-service unavailable"))
                        .build());
    }
}
