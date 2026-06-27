package com.temadison.drambuilder.dto;

import jakarta.validation.Valid;
import java.util.List;

public record MarketDataIngestionRequest(
        List<@Valid PriceSnapshotRequest> prices,
        List<@Valid FxRateSnapshotRequest> fxRates,
        List<@Valid OfficialNavSnapshotRequest> officialNavs,
        @Valid MarketDataSnapshotRequest snapshot
) {
}
