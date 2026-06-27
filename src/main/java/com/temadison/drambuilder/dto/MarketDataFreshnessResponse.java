package com.temadison.drambuilder.dto;

import java.time.Instant;
import java.util.List;

public record MarketDataFreshnessResponse(
        String status,
        Instant checkedAt,
        Instant staleBefore,
        long maxAgeHours,
        List<MarketDataPriceFreshnessResponse> requiredPrices
) {
}
