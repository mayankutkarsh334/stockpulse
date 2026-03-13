package com.stockmarket.resource;

import com.stockmarket.model.dto.request.AddTransactionRequest;
import com.stockmarket.model.dto.request.CreatePortfolioRequest;
import com.stockmarket.model.dto.response.PortfolioResponse;
import com.stockmarket.model.entity.Portfolio;
import com.stockmarket.model.entity.Transaction;
import com.stockmarket.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Singleton
@Path("/portfolios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Portfolio")
public class PortfolioResource {

    private final PortfolioService portfolioService;

    @Inject
    public PortfolioResource(final PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @POST
    @Operation(summary = "Create a new portfolio")
    public Response createPortfolio(@Valid final CreatePortfolioRequest req) {
        final var portfolio = portfolioService.createPortfolio(req);
        return Response.status(Response.Status.CREATED).entity(portfolio).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get portfolio with live P&L")
    public PortfolioResponse getPortfolio(@PathParam("id") final String id) {
        return portfolioService.getPortfolioWithPnL(id);
    }

    @POST
    @Path("/{id}/transactions")
    @Operation(summary = "Add a BUY or SELL transaction")
    public Response addTransaction(@PathParam("id") final String portfolioId,
                                   @Valid final AddTransactionRequest req) {
        final var txn = portfolioService.addTransaction(portfolioId, req);
        return Response.status(Response.Status.CREATED).entity(txn).build();
    }
}
