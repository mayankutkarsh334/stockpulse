package com.stockmarket.resource;

import com.stockmarket.model.dto.request.CsvAnalysisRequest;
import com.stockmarket.model.dto.response.CsvAnalysisResultResponse;
import com.stockmarket.service.CsvAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Singleton
@Path("/analysis/csv")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Analysis")
public class CsvAnalysisResource {

    private final CsvAnalysisService csvAnalysisService;

    @Inject
    public CsvAnalysisResource(final CsvAnalysisService csvAnalysisService) {
        this.csvAnalysisService = csvAnalysisService;
    }

    @POST
    @Path("/run")
    @Operation(summary = "Analyze stocks from a CSV screener export (e.g. Gemini Screener)")
    public CsvAnalysisResultResponse runCsvAnalysis(@Valid final CsvAnalysisRequest request) {
        return csvAnalysisService.analyze(request);
    }
}
