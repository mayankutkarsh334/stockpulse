package com.stockmarket.model.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualCloseRequest {
    @NotNull
    @Positive
    private BigDecimal closePrice;
    private String notes;
}
