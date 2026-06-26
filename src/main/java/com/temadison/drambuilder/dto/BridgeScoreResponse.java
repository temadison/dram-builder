package com.temadison.drambuilder.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public record BridgeScoreResponse(
        Long baselineSnapshotId,
        BigDecimal score,
        String rotationSignal,
        String recommendation,
        BigDecimal targetExposureWeight,
        BigDecimal premiumDiscountPercent,
        Set<String> targetTickers,
        boolean directSkHynixAvailable,
        BridgeScoreComponentResponse components,
        Instant createdAt
) {
}
