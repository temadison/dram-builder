package com.temadison.drambuilder.config;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.temadison.drambuilder.service.MarketDataFileIngestionService;
import com.temadison.drambuilder.service.MarketDataIngestionService;
import com.temadison.drambuilder.service.MarketDataProviderIngestionService;
import org.junit.jupiter.api.Test;

class ScheduledMarketDataIngestionJobTest {

    @Test
    void unsupportedScheduleModeRecordsFailedRun() {
        MarketDataIngestionService ingestionService = mock(MarketDataIngestionService.class);
        ScheduledMarketDataIngestionJob job = new ScheduledMarketDataIngestionJob(
                mock(MarketDataFileIngestionService.class),
                mock(MarketDataProviderIngestionService.class),
                ingestionService,
                "",
                "unsupported"
        );

        job.runMorningIngestion();

        verify(ingestionService).recordFailure(
                eq("scheduled-morning"),
                isNull(),
                org.mockito.ArgumentMatchers.any(IllegalArgumentException.class)
        );
    }
}
