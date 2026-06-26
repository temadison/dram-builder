package com.temadison.drambuilder.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceSnapshotResponse(
        Long id,
        String ticker,
        String name,
        String exchange,
        String currency,
        BigDecimal price,
        String source,
        Instant observedAt,
        Instant createdAt
) {
}
