package com.temadison.drambuilder.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record FxRateSnapshotResponse(
        Long id,
        String baseCurrency,
        String quoteCurrency,
        BigDecimal rate,
        String source,
        Instant observedAt,
        Instant createdAt
) {
}
