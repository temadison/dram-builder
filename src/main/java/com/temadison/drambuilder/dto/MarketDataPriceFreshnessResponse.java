package com.temadison.drambuilder.dto;

import java.time.Instant;
import java.time.LocalDate;

public record MarketDataPriceFreshnessResponse(
        String ticker,
        String exchange,
        Instant latestObservedAt,
        LocalDate expectedAsOfDate,
        String marketZone,
        String expectedAfterLocalTime,
        boolean missing,
        boolean stale
) {
}
