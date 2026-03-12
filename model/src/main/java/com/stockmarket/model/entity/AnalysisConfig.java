package com.stockmarket.model.entity;

import com.stockmarket.model.enums.AnalysisModelType;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisConfig {
    private String id;
    private String userId;
    private String name;
    private AnalysisModelType modelType;
    private String paramsJson;
    private LocalDateTime createdAt;
}
