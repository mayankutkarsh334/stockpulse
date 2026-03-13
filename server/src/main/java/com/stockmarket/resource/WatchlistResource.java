package com.stockmarket.resource;

import com.stockmarket.model.dto.request.CreateWatchlistRequest;
import com.stockmarket.model.dto.response.WatchlistResponse;
import com.stockmarket.model.entity.Watchlist;
import com.stockmarket.model.enums.Exchange;
import com.stockmarket.service.WatchlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Singleton
@Path("/watchlists")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Watchlist")
public class WatchlistResource {

    private final WatchlistService watchlistService;

    @Inject
    public WatchlistResource(final WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @POST
    @Operation(summary = "Create a watchlist")
    public Response createWatchlist(@Valid final CreateWatchlistRequest req) {
        final var wl = watchlistService.createWatchlist(req);
        return Response.status(Response.Status.CREATED).entity(wl).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get watchlist with live prices")
    public WatchlistResponse getWatchlist(@PathParam("id") final String id) {
        return watchlistService.getWatchlistWithPrices(id);
    }

    @POST
    @Path("/{id}/symbols")
    @Operation(summary = "Add symbol to watchlist")
    public Response addSymbol(@PathParam("id") final String watchlistId,
                              @QueryParam("symbol") final String symbol,
                              @QueryParam("exchange") final Exchange exchange) {
        watchlistService.addSymbol(watchlistId, symbol, exchange);
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}/symbols")
    @Operation(summary = "Remove symbol from watchlist")
    public Response removeSymbol(@PathParam("id") final String watchlistId,
                                 @QueryParam("symbol") final String symbol,
                                 @QueryParam("exchange") final Exchange exchange) {
        watchlistService.removeSymbol(watchlistId, symbol, exchange);
        return Response.noContent().build();
    }
}
