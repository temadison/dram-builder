package com.temadison.drambuilder.dto;

public record MarketDataSnapshotReadinessIssueResponse(
        String category,
        String key,
        String message
) {
}
