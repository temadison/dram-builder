package com.temadison.drambuilder.config;

import com.temadison.drambuilder.service.MarketDataFileIngestionService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.ingest.schedule", name = "enabled", havingValue = "true")
public class ScheduledMarketDataIngestionJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledMarketDataIngestionJob.class);

    private final MarketDataFileIngestionService marketDataFileIngestionService;
    private final String ingestionFile;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ScheduledMarketDataIngestionJob(
            MarketDataFileIngestionService marketDataFileIngestionService,
            @Value("${app.ingest.file:}") String ingestionFile
    ) {
        this.marketDataFileIngestionService = marketDataFileIngestionService;
        this.ingestionFile = ingestionFile;
    }

    @Scheduled(cron = "${app.ingest.schedule.morning-cron}", zone = "${app.ingest.schedule.zone}")
    public void runMorningIngestion() {
        runScheduledIngestion("morning");
    }

    @Scheduled(cron = "${app.ingest.schedule.evening-cron}", zone = "${app.ingest.schedule.zone}")
    public void runEveningIngestion() {
        runScheduledIngestion("evening");
    }

    private void runScheduledIngestion(String window) {
        if (!running.compareAndSet(false, true)) {
            LOGGER.warn("Skipping {} market data ingestion because another ingestion is already running", window);
            return;
        }

        try {
            LOGGER.info("Starting {} scheduled market data ingestion", window);
            marketDataFileIngestionService.ingestFile("scheduled-file-" + window, ingestionFile);
        } catch (Exception exception) {
            LOGGER.error("{} scheduled market data ingestion failed", window, exception);
        } finally {
            running.set(false);
        }
    }
}
