package com.stockmarket.resource;

import com.stockmarket.model.dto.request.AddTransactionRequest;
import com.stockmarket.model.dto.request.CreatePortfolioRequest;
import com.stockmarket.model.dto.response.PortfolioResponse;
import com.stockmarket.model.entity.Portfolio;
import com.stockmarket.model.entity.Transaction;
import com.stockmarket.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

@Path("/portfolios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Portfolio")
@RequiredArgsConstructor
public class PortfolioResource {

    private final PortfolioService portfolioService;

    @POST
    @Operation(summary = "Create a new portfolio")
    public Response createPortfolio(@Valid CreatePortfolioRequest req) {
        Portfolio portfolio = portfolioService.createPortfolio(req);
        return Response.status(Response.Status.CREATED).entity(portfolio).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get portfolio with live P&L")
    public PortfolioResponse getPortfolio(@PathParam("id") String id) {
        return portfolioService.getPortfolioWithPnL(id);
    }

    @POST
    @Path("/{id}/transactions")
    @Operation(summary = "Add a BUY or SELL transaction")
    public Response addTransaction(@PathParam("id") String portfolioId,
                                   @Valid AddTransactionRequest req) {
        Transaction txn = portfolioService.addTransaction(portfolioId, req);
        return Response.status(Response.Status.CREATED).entity(txn).build();
    }
}
