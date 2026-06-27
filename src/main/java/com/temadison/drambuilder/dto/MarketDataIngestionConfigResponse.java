package com.temadison.drambuilder.dto;

public record MarketDataIngestionConfigResponse(
        boolean runnerEnabled,
        String ingestionFile,
        boolean exitAfterRun,
        boolean scheduleEnabled,
        String scheduleMode,
        String scheduleZone,
        String morningCron,
        String eveningCron,
        int providerCount,
        long freshnessMaxAgeHours,
        String freshnessRequiredPrices
) {
}
