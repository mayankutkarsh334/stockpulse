package com.stockmarket.model.dto.request;

import com.stockmarket.model.enums.Exchange;
import com.stockmarket.model.enums.TransactionType;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddTransactionRequest {
    @NotBlank private String symbol;
    @NotNull private Exchange exchange;
    @NotNull private TransactionType type;
    @NotNull @Positive private BigDecimal quantity;
    @NotNull @Positive private BigDecimal price;
    private String notes;
    private LocalDateTime transactedAt;
}
