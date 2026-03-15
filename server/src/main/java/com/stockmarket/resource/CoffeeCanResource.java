package com.stockmarket.resource;

import com.stockmarket.model.dto.request.CoffeeCanScreenRequest;
import com.stockmarket.model.dto.response.CoffeeCanScreenResponse;
import com.stockmarket.service.CoffeeCanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Singleton
@Path("/coffee-can")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Coffee Can")
public class CoffeeCanResource {

    private final CoffeeCanService coffeeCanService;

    @Inject
    public CoffeeCanResource(final CoffeeCanService coffeeCanService) {
        this.coffeeCanService = coffeeCanService;
    }

    @POST
    @Path("/screen")
    @Operation(summary = "Screen stocks from CSV against Coffee Can criteria")
    public CoffeeCanScreenResponse screen(@Valid final CoffeeCanScreenRequest req) {
        return coffeeCanService.screen(req);
    }
}
