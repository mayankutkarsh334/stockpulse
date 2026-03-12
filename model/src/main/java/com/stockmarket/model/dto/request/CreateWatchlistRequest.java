package com.stockmarket.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateWatchlistRequest {
    @NotBlank private String userId;
    @NotBlank private String name;
}
