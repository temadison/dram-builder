package com.temadison.drambuilder.service;

import com.temadison.drambuilder.dto.MarketDataIngestionConfigResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MarketDataIngestionConfigService {

    private final boolean runnerEnabled;
    private final String ingestionFile;
    private final boolean exitAfterRun;
    private final boolean scheduleEnabled;
    private final String scheduleMode;
    private final String scheduleZone;
    private final String morningCron;
    private final String eveningCron;
    private final int providerCount;
    private final long freshnessMaxAgeHours;
    private final String freshnessRequiredPrices;

    public MarketDataIngestionConfigService(
            @Value("${app.ingest.enabled:false}") boolean runnerEnabled,
            @Value("${app.ingest.file:}") String ingestionFile,
            @Value("${app.ingest.exit-after-run:false}") boolean exitAfterRun,
            @Value("${app.ingest.schedule.enabled:false}") boolean scheduleEnabled,
            @Value("${app.ingest.schedule.mode:file}") String scheduleMode,
            @Value("${app.ingest.schedule.zone:America/Chicago}") String scheduleZone,
            @Value("${app.ingest.schedule.morning-cron:0 0 2 * * MON-FRI}") String morningCron,
            @Value("${app.ingest.schedule.evening-cron:0 30 16 * * MON-FRI}") String eveningCron,
            @Value("${app.market-data.freshness.max-age-hours:18}") long freshnessMaxAgeHours,
            @Value("${app.market-data.freshness.required-prices:BATS:DRAM,NASDAQ:MU,NASDAQ:SNDK,NASDAQ:WDC,NASDAQ:STX}") String freshnessRequiredPrices,
            List<MarketDataProvider> marketDataProviders
    ) {
        this.runnerEnabled = runnerEnabled;
        this.ingestionFile = ingestionFile;
        this.exitAfterRun = exitAfterRun;
        this.scheduleEnabled = scheduleEnabled;
        this.scheduleMode = scheduleMode;
        this.scheduleZone = scheduleZone;
        this.morningCron = morningCron;
        this.eveningCron = eveningCron;
        this.freshnessMaxAgeHours = freshnessMaxAgeHours;
        this.freshnessRequiredPrices = freshnessRequiredPrices;
        this.providerCount = marketDataProviders.size();
    }

    public MarketDataIngestionConfigResponse config() {
        return new MarketDataIngestionConfigResponse(
                runnerEnabled,
                ingestionFile == null || ingestionFile.isBlank() ? null : ingestionFile,
                exitAfterRun,
                scheduleEnabled,
                scheduleMode,
                scheduleZone,
                morningCron,
                eveningCron,
                providerCount,
                freshnessMaxAgeHours,
                freshnessRequiredPrices
        );
    }
}
