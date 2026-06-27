package com.temadison.drambuilder.dto;

import java.time.Instant;

public record MarketDataPriceFreshnessResponse(
        String ticker,
        String exchange,
        Instant latestObservedAt,
        boolean missing,
        boolean stale
) {
}
