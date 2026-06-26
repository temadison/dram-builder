package com.temadison.drambuilder.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.Map;

public record ScenarioRequest(
        String name,
        Map<String, BigDecimal> securityMovesPercent,
        Map<String, BigDecimal> fxMovesPercent,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal purchasePrice
) {
}
