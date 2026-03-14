package com.stockmarket.resource;

import com.stockmarket.model.dto.request.CreatePickRequest;
import com.stockmarket.model.dto.request.ManualCloseRequest;
import com.stockmarket.model.dto.response.PositionResponse;
import com.stockmarket.model.enums.CloseReason;
import com.stockmarket.model.enums.PositionStatus;
import com.stockmarket.service.InvestmentPositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Singleton
@Path("/strategy/positions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Strategy")
public class InvestmentPositionResource {

    private final InvestmentPositionService positionService;

    @Inject
    public InvestmentPositionResource(final InvestmentPositionService positionService) {
        this.positionService = positionService;
    }

    @POST
    @Operation(summary = "Create this month's pick — invest ₹20,000 in top-ranked stock")
    public Response createMonthlyPick(@Valid final CreatePickRequest req) {
        final var position = positionService.createMonthlyPick(req);
        return Response.status(201)
                .entity(positionService.getPositionById(position.getId()).orElse(null))
                .build();
    }

    @GET
    @Operation(summary = "List positions, optionally filtered by status")
    public List<PositionResponse> listPositions(@QueryParam("status") final String status) {
        return positionService.listPositions(status);
    }

    @GET
    @Path("/current")
    @Operation(summary = "Get the current month's position (204 if none)")
    public Response getCurrentPosition() {
        return positionService.getCurrentMonthPosition()
                .map(p -> Response.ok(p).build())
                .orElse(Response.noContent().build());
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get a position by ID")
    public Response getPosition(@PathParam("id") final String id) {
        return positionService.getPositionById(id)
                .map(p -> Response.ok(p).build())
                .orElse(Response.status(404).build());
    }

    @PUT
    @Path("/{id}/close")
    @Operation(summary = "Manually close a position")
    public Response manualClose(@PathParam("id") final String id,
                                @Valid final ManualCloseRequest req) {
        positionService.closePosition(id, req.getClosePrice(), CloseReason.MANUAL, PositionStatus.MANUALLY_CLOSED);
        return positionService.getPositionById(id)
                .map(p -> Response.ok(p).build())
                .orElse(Response.status(404).build());
    }
}
