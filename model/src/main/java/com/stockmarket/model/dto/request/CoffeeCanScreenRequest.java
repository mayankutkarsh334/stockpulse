package com.stockmarket.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoffeeCanScreenRequest {
    @NotBlank
    private String csvContent;
}
