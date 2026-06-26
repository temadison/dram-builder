package com.temadison.drambuilder.service;

import java.math.BigDecimal;
import java.util.List;

public record AttributionResult(
        boolean hasPriorSnapshot,
        Long currentSnapshotId,
        Long priorSnapshotId,
        BigDecimal syntheticNavChangePercent,
        BigDecimal marketPriceChangePercent,
        List<HoldingAttribution> topContributors
) {
}
