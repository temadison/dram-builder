package com.temadison.drambuilder.dto;

import java.time.Instant;

public record MarketDataIngestionRunResponse(
        Long id,
        String source,
        String status,
        String requestedFile,
        int pricesImported,
        int fxRatesImported,
        int officialNavsImported,
        boolean snapshotCreated,
        String message,
        Instant startedAt,
        Instant completedAt
) {
}
