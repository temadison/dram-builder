package com.temadison.drambuilder.dto;

import java.util.List;

public record MarketDataSummaryResponse(
        List<PriceSnapshotResponse> latestPrices,
        List<FxRateSnapshotResponse> latestFxRates
) {
}
