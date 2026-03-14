package com.stockmarket.model.dto.request;

import com.stockmarket.model.enums.AnalysisModelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CsvAnalysisRequest {
    @NotNull @NotBlank
    private String csvContent;

    private AnalysisModelType modelType = AnalysisModelType.WEIGHTED_SCORE;
    private Map<String, Double> weights;
    private List<String> invertMetrics;
    private String formula;
}
