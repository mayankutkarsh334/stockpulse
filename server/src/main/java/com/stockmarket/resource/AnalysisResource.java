package com.stockmarket.resource;

import com.stockmarket.model.dto.request.AnalysisRequest;
import com.stockmarket.model.dto.response.AnalysisResultResponse;
import com.stockmarket.model.entity.AnalysisConfig;
import com.stockmarket.model.enums.AnalysisModelType;
import com.stockmarket.service.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Path("/analysis")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Analysis")
@RequiredArgsConstructor
public class AnalysisResource {

    private final AnalysisService analysisService;

    @POST
    @Path("/run")
    @Operation(summary = "Run analysis model on a list of stocks")
    public AnalysisResultResponse runAnalysis(@Valid AnalysisRequest req) {
        return analysisService.runAnalysis(req);
    }

    @GET
    @Path("/models")
    @Operation(summary = "List available analysis models")
    public List<AnalysisModelType> getModels() {
        return analysisService.getAvailableModels();
    }

    @POST
    @Path("/configs")
    @Operation(summary = "Save an analysis configuration")
    public Response saveConfig(@QueryParam("userId") String userId,
                               @QueryParam("name") String name,
                               @Valid AnalysisRequest req) {
        AnalysisConfig config = analysisService.saveConfig(userId, name, req.getModelType(), req.getParams());
        return Response.status(Response.Status.CREATED).entity(config).build();
    }

    @GET
    @Path("/configs/{id}")
    @Operation(summary = "Retrieve a saved analysis configuration")
    public AnalysisConfig getConfig(@PathParam("id") String configId) {
        return analysisService.getConfig(configId);
    }
}
