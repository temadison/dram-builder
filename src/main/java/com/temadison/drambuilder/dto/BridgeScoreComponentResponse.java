package com.temadison.drambuilder.dto;

import java.math.BigDecimal;

public record BridgeScoreComponentResponse(
        BigDecimal targetExposureScore,
        BigDecimal premiumDiscountScore,
        BigDecimal liquidityScore,
        BigDecimal trackingConfidenceScore,
        BigDecimal timingRiskScore
) {
}
