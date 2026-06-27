package com.temadison.drambuilder.service;

import com.temadison.drambuilder.domain.MarketDataIngestionRun;
import com.temadison.drambuilder.dto.MarketDataIngestionRunResponse;
import com.temadison.drambuilder.repository.MarketDataIngestionRunRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketDataIngestionRunService {

    private final MarketDataIngestionRunRepository marketDataIngestionRunRepository;

    public MarketDataIngestionRunService(MarketDataIngestionRunRepository marketDataIngestionRunRepository) {
        this.marketDataIngestionRunRepository = marketDataIngestionRunRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MarketDataIngestionRun startFileRun(String requestedFile) {
        return marketDataIngestionRunRepository.save(new MarketDataIngestionRun("file", requestedFile, Instant.now()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(Long runId, int pricesImported, int fxRatesImported, int officialNavsImported, boolean snapshotCreated) {
        MarketDataIngestionRun run = marketDataIngestionRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalStateException("No ingestion run exists for id " + runId));
        run.complete(
                pricesImported,
                fxRatesImported,
                officialNavsImported,
                snapshotCreated,
                "Market data ingestion completed",
                Instant.now()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long runId, Exception exception) {
        MarketDataIngestionRun run = marketDataIngestionRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalStateException("No ingestion run exists for id " + runId));
        run.fail(exception.getMessage(), Instant.now());
    }

    @Transactional(readOnly = true)
    public List<MarketDataIngestionRunResponse> recentRuns() {
        return marketDataIngestionRunRepository.findTop10ByOrderByStartedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private MarketDataIngestionRunResponse toResponse(MarketDataIngestionRun run) {
        return new MarketDataIngestionRunResponse(
                run.getId(),
                run.getSource(),
                run.getStatus(),
                run.getRequestedFile(),
                run.getPricesImported(),
                run.getFxRatesImported(),
                run.getOfficialNavsImported(),
                run.isSnapshotCreated(),
                run.getMessage(),
                run.getStartedAt(),
                run.getCompletedAt()
        );
    }
}
