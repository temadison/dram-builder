package com.temadison.drambuilder.dto;

import java.math.BigDecimal;
import java.util.List;

public record AttributionResponse(
        boolean hasPriorSnapshot,
        Long currentSnapshotId,
        Long priorSnapshotId,
        BigDecimal syntheticNavChangePercent,
        BigDecimal marketPriceChangePercent,
        List<HoldingAttributionResponse> topContributors
) {
}
