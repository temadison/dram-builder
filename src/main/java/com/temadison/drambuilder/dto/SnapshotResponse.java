package com.temadison.drambuilder.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SnapshotResponse(
        Long snapshotId,
        String etfTicker,
        LocalDate asOfDate,
        BigDecimal marketPrice,
        BigDecimal purchasePrice,
        BigDecimal syntheticNav,
        BigDecimal estimatedEtfMovePercent,
        BigDecimal premiumDiscountPercent,
        Instant createdAt,
        List<HoldingResult> holdings,
        AttributionResponse attribution
) {
}
