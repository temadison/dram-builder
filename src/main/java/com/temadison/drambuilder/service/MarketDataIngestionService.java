package com.temadison.drambuilder.service;

import com.temadison.drambuilder.dto.BulkMarketDataImportRequest;
import com.temadison.drambuilder.dto.FxRateSnapshotRequest;
import com.temadison.drambuilder.dto.MarketDataIngestionRequest;
import com.temadison.drambuilder.dto.OfficialNavSnapshotRequest;
import com.temadison.drambuilder.dto.PriceSnapshotRequest;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MarketDataIngestionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketDataIngestionService.class);

    private final Validator validator;
    private final MarketDataService marketDataService;
    private final DramMarketDataSnapshotService dramMarketDataSnapshotService;
    private final MarketDataIngestionRunService marketDataIngestionRunService;

    public MarketDataIngestionService(
            Validator validator,
            MarketDataService marketDataService,
            DramMarketDataSnapshotService dramMarketDataSnapshotService,
            MarketDataIngestionRunService marketDataIngestionRunService
    ) {
        this.validator = validator;
        this.marketDataService = marketDataService;
        this.dramMarketDataSnapshotService = dramMarketDataSnapshotService;
        this.marketDataIngestionRunService = marketDataIngestionRunService;
    }

    public void ingest(String source, String requestedFile, MarketDataIngestionRequest request) {
        Long runId = marketDataIngestionRunService.startRun(source, requestedFile).getId();

        try {
            validate(request);
            IngestionCounts counts = ingest(request);
            marketDataIngestionRunService.complete(
                    runId,
                    counts.pricesImported(),
                    counts.fxRatesImported(),
                    counts.officialNavsImported(),
                    counts.snapshotCreated()
            );
        } catch (RuntimeException exception) {
            marketDataIngestionRunService.fail(runId, exception);
            throw exception;
        }
    }

    public void recordFailure(String source, String requestedFile, Exception exception) {
        Long runId = marketDataIngestionRunService.startRun(source, requestedFile).getId();
        marketDataIngestionRunService.fail(runId, exception);
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
