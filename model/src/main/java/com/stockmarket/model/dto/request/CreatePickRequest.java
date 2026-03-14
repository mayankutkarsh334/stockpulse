package com.stockmarket.model.dto.request;

import com.stockmarket.model.enums.AnalysisModelType;
import com.stockmarket.model.enums.Exchange;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePickRequest {
    @NotBlank
    private String symbol;
    @NotBlank
    private String companyName;
    private String sector;
    @NotNull
    private Exchange exchange;
    @NotNull
    @Positive
    private BigDecimal entryPrice;
    @NotNull
    private AnalysisModelType modelType;
}
