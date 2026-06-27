package com.temadison.drambuilder.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record OfficialNavSnapshotResponse(
        Long id,
        String ticker,
        String name,
        BigDecimal nav,
        String currency,
        String source,
        LocalDate asOfDate,
        Instant observedAt,
        Instant createdAt
) {
}
