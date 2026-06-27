package com.temadison.drambuilder.config;

import com.temadison.drambuilder.service.MarketDataFileIngestionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.ingest", name = "enabled", havingValue = "true")
public class MarketDataIngestionRunner implements CommandLineRunner {

    private final ConfigurableApplicationContext applicationContext;
    private final MarketDataFileIngestionService marketDataFileIngestionService;
    private final String ingestionFile;
    private final boolean exitAfterRun;

    public MarketDataIngestionRunner(
            ConfigurableApplicationContext applicationContext,
            MarketDataFileIngestionService marketDataFileIngestionService,
            @Value("${app.ingest.file:}") String ingestionFile,
            @Value("${app.ingest.exit-after-run:false}") boolean exitAfterRun
    ) {
        this.applicationContext = applicationContext;
        this.marketDataFileIngestionService = marketDataFileIngestionService;
        this.ingestionFile = ingestionFile;
        this.exitAfterRun = exitAfterRun;
    }

    @Override
    public void run(String... args) throws Exception {
        marketDataFileIngestionService.ingestFile("file", ingestionFile);

        if (exitAfterRun) {
            Thread shutdownThread = new Thread(() -> SpringApplication.exit(applicationContext, () -> 0));
            shutdownThread.setName("market-data-ingestion-shutdown");
            shutdownThread.setDaemon(false);
            shutdownThread.start();
        }
    }
}
