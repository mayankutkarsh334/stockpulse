package com.stockmarket.resource;

import com.stockmarket.model.dto.request.ScreenerScanRequest;
import com.stockmarket.model.dto.response.ScreenerResultResponse;
import com.stockmarket.service.ScreenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Singleton
@Path("/screener")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Screener")
public class ScreenerResource {

    private final ScreenerService screenerService;

    @Inject
    public ScreenerResource(final ScreenerService screenerService) {
        this.screenerService = screenerService;
    }

    @POST
    @Path("/scan")
    @Operation(summary = "Filter stocks by criteria")
    public ScreenerResultResponse scan(@Valid final ScreenerScanRequest req) {
        return screenerService.scan(req);
    }

    @GET
    @Path("/presets")
    @Operation(summary = "List available screener presets")
    public List<String> getPresets() {
        return screenerService.getAvailablePresets();
    }
}
