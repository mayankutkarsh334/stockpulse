package com.stockmarket.model.dto.request;

import com.stockmarket.model.enums.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePortfolioRequest {
    @NotBlank private String userId;
    @NotBlank private String name;
    @NotNull private Currency currency;
}
