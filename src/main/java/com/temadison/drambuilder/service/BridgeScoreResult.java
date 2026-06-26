package com.temadison.drambuilder.service;

import java.math.BigDecimal;

public record BridgeScoreResult(
        BigDecimal score,
        BigDecimal targetExposureScore,
        BigDecimal premiumDiscountScore,
        BigDecimal liquidityScore,
        BigDecimal trackingConfidenceScore,
        BigDecimal timingRiskScore,
        RotationSignal rotationSignal,
        String recommendation
) {
}
