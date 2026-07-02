package com.temadison.drambuilder.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record MarketDataFreshnessResponse(
        String status,
        Instant checkedAt,
        Instant staleBefore,
        long maxAgeHours,
        LocalDate expectedAsOfDate,
        String marketZone,
        String expectedAfterLocalTime,
        List<MarketDataPriceFreshnessResponse> requiredPrices
) {
}
