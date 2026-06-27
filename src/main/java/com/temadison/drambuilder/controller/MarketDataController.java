package com.temadison.drambuilder.controller;

import com.temadison.drambuilder.dto.BulkMarketDataImportRequest;
import com.temadison.drambuilder.dto.BulkMarketDataImportResponse;
import com.temadison.drambuilder.dto.FxRateSnapshotRequest;
import com.temadison.drambuilder.dto.FxRateSnapshotResponse;
import com.temadison.drambuilder.dto.MarketDataSummaryResponse;
import com.temadison.drambuilder.dto.PriceSnapshotRequest;
import com.temadison.drambuilder.dto.PriceSnapshotResponse;
import com.temadison.drambuilder.service.MarketDataService;
import jakarta.validation.Valid;
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

    public MarketDataController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping
    public MarketDataSummaryResponse summary() {
        return marketDataService.summary();
    }

    @PostMapping("/prices")
    public PriceSnapshotResponse createPriceSnapshot(@Valid @RequestBody PriceSnapshotRequest request) {
        return marketDataService.createPriceSnapshot(request);
    }

    @PostMapping("/import")
    public BulkMarketDataImportResponse importMarketData(@Valid @RequestBody BulkMarketDataImportRequest request) {
        return marketDataService.importMarketData(request);
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
}
