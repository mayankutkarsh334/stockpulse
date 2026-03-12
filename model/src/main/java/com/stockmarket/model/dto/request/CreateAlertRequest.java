package com.stockmarket.model.dto.request;

import com.stockmarket.model.enums.AlertDirection;
import com.stockmarket.model.enums.Exchange;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAlertRequest {
    @NotBlank private String userId;
    @NotBlank private String symbol;
    @NotNull private Exchange exchange;
    @NotNull @Positive private BigDecimal targetPrice;
    @NotNull private AlertDirection direction;
}
