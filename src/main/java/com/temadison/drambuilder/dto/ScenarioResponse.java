package com.temadison.drambuilder.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ScenarioResponse(
        Long scenarioRunId,
        Long baselineSnapshotId,
        String name,
        BigDecimal baselineMarketPrice,
        BigDecimal purchasePrice,
        BigDecimal estimatedMovePercent,
        BigDecimal projectedMarketPrice,
        BigDecimal dollarImpactVsPurchasePrice,
        Instant createdAt,
        List<ScenarioHoldingResponse> holdings
) {
}
