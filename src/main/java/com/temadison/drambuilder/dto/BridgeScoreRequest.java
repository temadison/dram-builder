package com.temadison.drambuilder.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.Set;

public record BridgeScoreRequest(
        Set<String> targetTickers,
        @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal liquidityScore,
        @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal trackingConfidenceScore,
        @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal timingRiskScore,
        boolean directSkHynixAvailable
) {
}
