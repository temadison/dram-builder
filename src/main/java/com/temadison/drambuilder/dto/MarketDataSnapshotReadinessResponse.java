package com.temadison.drambuilder.dto;

import java.time.LocalDate;
import java.util.List;

public record MarketDataSnapshotReadinessResponse(
        String status,
        LocalDate asOfDate,
        boolean officialNavPresent,
        List<MarketDataSnapshotReadinessIssueResponse> issues
) {
}
