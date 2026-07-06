package com.temadison.drambuilder.service;

import com.temadison.drambuilder.config.MarketDataFreshnessProperties;
import com.temadison.drambuilder.dto.MarketDataIngestionConfigResponse;
import java.util.List;
import java.util.stream.Collectors;
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
    private final String freshnessMarketZone;
    private final String freshnessExpectedAfterLocalTime;
    private final String freshnessMarketHolidays;
    private final String freshnessExchangeCalendars;
    private final String freshnessRequiredPrices;

    public MarketDataIngestionConfigService(
            @Value("${app.ingest.enabled:false}") boolean runnerEnabled,
            @Value("${app.ingest.file:}") String ingestionFile,
            @Value("${app.ingest.exit-after-run:false}") boolean exitAfterRun,
            @Value("${app.ingest.schedule.enabled:false}") boolean scheduleEnabled,
            @Value("${app.ingest.schedule.mode:file}") String scheduleMode,
            @Value("${app.ingest.schedule.zone:America/Chicago}") String scheduleZone,
            @Value("${app.ingest.schedule.morning-cron:0 30 4 * * MON-FRI}") String morningCron,
            @Value("${app.ingest.schedule.evening-cron:0 30 16 * * MON-FRI}") String eveningCron,
            MarketDataFreshnessProperties freshnessProperties,
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
        this.freshnessMaxAgeHours = freshnessProperties.getMaxAgeHours();
        this.freshnessMarketZone = freshnessProperties.getMarketZone();
        this.freshnessExpectedAfterLocalTime = freshnessProperties.getExpectedAfterLocalTime();
        this.freshnessMarketHolidays = freshnessProperties.getMarketHolidays();
        this.freshnessExchangeCalendars = exchangeCalendarSummary(freshnessProperties);
        this.freshnessRequiredPrices = freshnessProperties.getRequiredPrices();
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
                freshnessMarketZone,
                freshnessExpectedAfterLocalTime,
                freshnessMarketHolidays,
                freshnessExchangeCalendars,
                freshnessRequiredPrices
        );
    }

    private String exchangeCalendarSummary(MarketDataFreshnessProperties properties) {
        if (properties.getExchangeCalendars().isEmpty()) {
            return "";
        }
        return properties.getExchangeCalendars().entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue().getMarketZone()
                        + "@" + entry.getValue().getExpectedAfterLocalTime())
                .collect(Collectors.joining(","));
    }
}
