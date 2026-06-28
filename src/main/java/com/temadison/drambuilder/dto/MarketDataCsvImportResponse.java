package com.temadison.drambuilder.dto;

import java.util.List;

public record MarketDataCsvImportResponse(
        int pricesImported,
        int fxRatesImported,
        int officialNavsImported,
        List<PriceSnapshotResponse> prices,
        List<FxRateSnapshotResponse> fxRates,
        List<OfficialNavSnapshotResponse> officialNavs
) {
}
