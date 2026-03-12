package com.stockmarket.resource;

import com.stockmarket.model.dto.request.CreateWatchlistRequest;
import com.stockmarket.model.dto.response.WatchlistResponse;
import com.stockmarket.model.entity.Watchlist;
import com.stockmarket.model.enums.Exchange;
import com.stockmarket.service.WatchlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

@Path("/watchlists")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Watchlist")
@RequiredArgsConstructor
public class WatchlistResource {

    private final WatchlistService watchlistService;

    @POST
    @Operation(summary = "Create a watchlist")
    public Response createWatchlist(@Valid CreateWatchlistRequest req) {
        Watchlist wl = watchlistService.createWatchlist(req);
        return Response.status(Response.Status.CREATED).entity(wl).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get watchlist with live prices")
    public WatchlistResponse getWatchlist(@PathParam("id") String id) {
        return watchlistService.getWatchlistWithPrices(id);
    }

    @POST
    @Path("/{id}/symbols")
    @Operation(summary = "Add symbol to watchlist")
    public Response addSymbol(@PathParam("id") String watchlistId,
                              @QueryParam("symbol") String symbol,
                              @QueryParam("exchange") Exchange exchange) {
        watchlistService.addSymbol(watchlistId, symbol, exchange);
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}/symbols")
    @Operation(summary = "Remove symbol from watchlist")
    public Response removeSymbol(@PathParam("id") String watchlistId,
                                 @QueryParam("symbol") String symbol,
                                 @QueryParam("exchange") Exchange exchange) {
        watchlistService.removeSymbol(watchlistId, symbol, exchange);
        return Response.noContent().build();
    }
}
