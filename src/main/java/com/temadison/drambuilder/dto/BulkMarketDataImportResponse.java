package com.temadison.drambuilder.dto;

import java.util.List;

public record BulkMarketDataImportResponse(
        int pricesImported,
        int fxRatesImported,
        List<PriceSnapshotResponse> prices,
        List<FxRateSnapshotResponse> fxRates
) {
}
