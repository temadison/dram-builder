package com.temadison.drambuilder.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temadison.drambuilder.dto.BulkMarketDataImportRequest;
import com.temadison.drambuilder.dto.FxRateSnapshotRequest;
import com.temadison.drambuilder.dto.MarketDataIngestionRequest;
import com.temadison.drambuilder.dto.OfficialNavSnapshotRequest;
import com.temadison.drambuilder.dto.PriceSnapshotRequest;
import com.temadison.drambuilder.service.DramMarketDataSnapshotService;
import com.temadison.drambuilder.service.MarketDataIngestionRunService;
import com.temadison.drambuilder.service.MarketDataService;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.ingest", name = "enabled", havingValue = "true")
public class MarketDataIngestionRunner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketDataIngestionRunner.class);

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final Validator validator;
    private final ConfigurableApplicationContext applicationContext;
    private final MarketDataService marketDataService;
    private final DramMarketDataSnapshotService dramMarketDataSnapshotService;
    private final MarketDataIngestionRunService marketDataIngestionRunService;
    private final String ingestionFile;
    private final boolean exitAfterRun;

    public MarketDataIngestionRunner(
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader,
            Validator validator,
            ConfigurableApplicationContext applicationContext,
            MarketDataService marketDataService,
            DramMarketDataSnapshotService dramMarketDataSnapshotService,
            MarketDataIngestionRunService marketDataIngestionRunService,
            @Value("${app.ingest.file:}") String ingestionFile,
            @Value("${app.ingest.exit-after-run:false}") boolean exitAfterRun
    ) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.validator = validator;
        this.applicationContext = applicationContext;
        this.marketDataService = marketDataService;
        this.dramMarketDataSnapshotService = dramMarketDataSnapshotService;
        this.marketDataIngestionRunService = marketDataIngestionRunService;
        this.ingestionFile = ingestionFile;
        this.exitAfterRun = exitAfterRun;
    }

    @Override
    public void run(String... args) throws Exception {
        if (ingestionFile == null || ingestionFile.isBlank()) {
            throw new IllegalArgumentException("app.ingest.file is required when app.ingest.enabled=true");
        }

        Long runId = marketDataIngestionRunService.startFileRun(ingestionFile).getId();

        try {
            Resource resource = resourceLoader.getResource(ingestionFile);
            if (!resource.exists()) {
                throw new IllegalArgumentException("Ingestion file does not exist: " + ingestionFile);
            }

            MarketDataIngestionRequest request;
            try (InputStream inputStream = resource.getInputStream()) {
                request = objectMapper.readValue(inputStream, MarketDataIngestionRequest.class);
            }
            validate(request);
            IngestionCounts counts = ingest(request);
            marketDataIngestionRunService.complete(
                    runId,
                    counts.pricesImported(),
                    counts.fxRatesImported(),
                    counts.officialNavsImported(),
                    counts.snapshotCreated()
            );
        } catch (Exception exception) {
            marketDataIngestionRunService.fail(runId, exception);
            throw exception;
        }

        if (exitAfterRun) {
            Thread shutdownThread = new Thread(() -> SpringApplication.exit(applicationContext, () -> 0));
            shutdownThread.setName("market-data-ingestion-shutdown");
            shutdownThread.setDaemon(false);
            shutdownThread.start();
        }
    }

    private void validate(MarketDataIngestionRequest request) {
        String message = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        if (!message.isBlank()) {
            throw new IllegalArgumentException("Invalid ingestion file: " + message);
        }
    }

    private IngestionCounts ingest(@Valid MarketDataIngestionRequest request) {
        List<PriceSnapshotRequest> prices = request.prices() == null ? List.of() : request.prices();
        List<FxRateSnapshotRequest> fxRates = request.fxRates() == null ? List.of() : request.fxRates();
        List<OfficialNavSnapshotRequest> officialNavs = request.officialNavs() == null ? List.of() : request.officialNavs();

        if (!prices.isEmpty() || !fxRates.isEmpty()) {
            marketDataService.importMarketData(new BulkMarketDataImportRequest(prices, fxRates));
        }

        officialNavs.forEach(marketDataService::createOfficialNavSnapshot);

        if (request.snapshot() != null) {
            dramMarketDataSnapshotService.createSnapshot(request.snapshot());
        }

        LOGGER.info(
                "Market data ingestion completed: {} prices, {} FX rates, {} official NAVs, snapshot={}",
                prices.size(),
                fxRates.size(),
                officialNavs.size(),
                request.snapshot() != null
        );
        return new IngestionCounts(prices.size(), fxRates.size(), officialNavs.size(), request.snapshot() != null);
    }

    private record IngestionCounts(
            int pricesImported,
            int fxRatesImported,
            int officialNavsImported,
            boolean snapshotCreated
    ) {
    }
}
