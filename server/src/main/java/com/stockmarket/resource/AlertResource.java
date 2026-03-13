package com.stockmarket.resource;

import com.stockmarket.model.dto.request.CreateAlertRequest;
import com.stockmarket.model.dto.response.AlertResponse;
import com.stockmarket.model.enums.AlertStatus;
import com.stockmarket.service.AlertService;
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
@Path("/alerts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Alert")
public class AlertResource {

    private final AlertService alertService;

    @Inject
    public AlertResource(final AlertService alertService) {
        this.alertService = alertService;
    }

    @POST
    @Operation(summary = "Create a price alert")
    public Response createAlert(@Valid final CreateAlertRequest req) {
        final var alert = alertService.createAlert(req);
        return Response.status(Response.Status.CREATED).entity(alert).build();
    }

    @GET
    @Operation(summary = "List alerts by user and status")
    public List<AlertResponse> getAlerts(@QueryParam("userId") final String userId,
                                          @QueryParam("status") @DefaultValue("ACTIVE") final AlertStatus status) {
        return alertService.getAlertsByUser(userId, status);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Cancel an alert")
    public Response cancelAlert(@PathParam("id") final String alertId) {
        alertService.cancelAlert(alertId);
        return Response.noContent().build();
    }
}
