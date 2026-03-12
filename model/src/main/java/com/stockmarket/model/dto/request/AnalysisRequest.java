package com.stockmarket.model.dto.request;

import com.stockmarket.model.enums.AnalysisModelType;
import com.stockmarket.model.enums.Exchange;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisRequest {
    @NotNull private List<String> symbols;
    @NotNull private Exchange exchange;
    @NotNull private AnalysisModelType modelType;
    private Map<String, Object> params;
    private String configId;
}
