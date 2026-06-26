package com.temadison.drambuilder.service;

import java.math.BigDecimal;

public record BridgeScoreInput(
        BigDecimal targetExposureWeight,
        BigDecimal premiumDiscountPercent,
        BigDecimal liquidityScore,
        BigDecimal trackingConfidenceScore,
        BigDecimal timingRiskScore,
        boolean directSkHynixAvailable
) {
}
