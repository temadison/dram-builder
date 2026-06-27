package com.temadison.drambuilder.controller;

import com.temadison.drambuilder.dto.BulkMarketDataImportRequest;
import com.temadison.drambuilder.dto.BulkMarketDataImportResponse;
import com.temadison.drambuilder.dto.FxRateSnapshotRequest;
import com.temadison.drambuilder.dto.FxRateSnapshotResponse;
import com.temadison.drambuilder.dto.MarketDataIngestionConfigResponse;
import com.temadison.drambuilder.dto.MarketDataIngestionRunResponse;
import com.temadison.drambuilder.dto.MarketDataSummaryResponse;
import com.temadison.drambuilder.dto.OfficialNavSnapshotRequest;
import com.temadison.drambuilder.dto.OfficialNavSnapshotResponse;
import com.temadison.drambuilder.dto.PriceSnapshotRequest;
import com.temadison.drambuilder.dto.PriceSnapshotResponse;
import com.temadison.drambuilder.service.MarketDataCsvImportService;
import com.temadison.drambuilder.service.MarketDataIngestionConfigService;
import com.temadison.drambuilder.service.MarketDataIngestionRunService;
import com.temadison.drambuilder.service.MarketDataService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market-data")
public class MarketDataController {

    private final MarketDataService marketDataService;
    private final MarketDataCsvImportService marketDataCsvImportService;
    private final MarketDataIngestionRunService marketDataIngestionRunService;
    private final MarketDataIngestionConfigService marketDataIngestionConfigService;

    public MarketDataController(
            MarketDataService marketDataService,
            MarketDataCsvImportService marketDataCsvImportService,
            MarketDataIngestionRunService marketDataIngestionRunService,
            MarketDataIngestionConfigService marketDataIngestionConfigService
    ) {
        this.marketDataService = marketDataService;
        this.marketDataCsvImportService = marketDataCsvImportService;
        this.marketDataIngestionRunService = marketDataIngestionRunService;
        this.marketDataIngestionConfigService = marketDataIngestionConfigService;
    }

    @GetMapping
    public MarketDataSummaryResponse summary() {
        return marketDataService.summary();
    }

    @GetMapping("/ingestion-runs")
    public List<MarketDataIngestionRunResponse> ingestionRuns() {
        return marketDataIngestionRunService.recentRuns();
    }

    @GetMapping("/ingestion-config")
    public MarketDataIngestionConfigResponse ingestionConfig() {
        return marketDataIngestionConfigService.config();
    }

    @PostMapping("/prices")
    public PriceSnapshotResponse createPriceSnapshot(@Valid @RequestBody PriceSnapshotRequest request) {
        return marketDataService.createPriceSnapshot(request);
    }

    @PostMapping("/import")
    public BulkMarketDataImportResponse importMarketData(@Valid @RequestBody BulkMarketDataImportRequest request) {
        return marketDataService.importMarketData(request);
    }

    @PostMapping(value = "/import/csv", consumes = { "text/csv", MediaType.TEXT_PLAIN_VALUE })
    public BulkMarketDataImportResponse importMarketDataCsv(@RequestBody String csv) {
        return marketDataCsvImportService.importCsv(csv);
    }

    @GetMapping("/prices/{exchange}/{ticker}/latest")
    public PriceSnapshotResponse latestPrice(@PathVariable String exchange, @PathVariable String ticker) {
        return marketDataService.latestPrice(ticker, exchange);
    }

    @PostMapping("/fx-rates")
    public FxRateSnapshotResponse createFxRateSnapshot(@Valid @RequestBody FxRateSnapshotRequest request) {
        return marketDataService.createFxRateSnapshot(request);
    }

    @GetMapping("/fx-rates/{baseCurrency}/{quoteCurrency}/latest")
    public FxRateSnapshotResponse latestFxRate(@PathVariable String baseCurrency, @PathVariable String quoteCurrency) {
        return marketDataService.latestFxRate(baseCurrency, quoteCurrency);
    }

    @PostMapping("/official-navs")
    public OfficialNavSnapshotResponse createOfficialNavSnapshot(@Valid @RequestBody OfficialNavSnapshotRequest request) {
        return marketDataService.createOfficialNavSnapshot(request);
    }

    @GetMapping("/official-navs/{ticker}/latest")
    public OfficialNavSnapshotResponse latestOfficialNav(@PathVariable String ticker) {
        return marketDataService.latestOfficialNav(ticker);
    }
}
