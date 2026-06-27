package com.temadison.drambuilder.dto;

import jakarta.validation.Valid;
import java.util.List;

public record BulkMarketDataImportRequest(
        List<@Valid PriceSnapshotRequest> prices,
        List<@Valid FxRateSnapshotRequest> fxRates
) {
}
