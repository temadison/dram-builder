package com.temadison.drambuilder.service;

import com.temadison.drambuilder.dto.MarketDataIngestionRequest;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MarketDataProviderIngestionService {

    private final List<MarketDataProvider> marketDataProviders;
    private final MarketDataIngestionService marketDataIngestionService;

    public MarketDataProviderIngestionService(
            List<MarketDataProvider> marketDataProviders,
            MarketDataIngestionService marketDataIngestionService
    ) {
        this.marketDataProviders = marketDataProviders;
        this.marketDataIngestionService = marketDataIngestionService;
    }

    public void ingestProvider(String window) {
        String source = "provider-" + window;
        if (marketDataProviders.isEmpty()) {
            IllegalStateException exception = new IllegalStateException("No market data provider is configured");
            marketDataIngestionService.recordFailure(source, null, exception);
            throw exception;
        }

        if (marketDataProviders.size() > 1) {
            IllegalStateException exception = new IllegalStateException("More than one market data provider is configured");
            marketDataIngestionService.recordFailure(source, null, exception);
            throw exception;
        }

        MarketDataProvider provider = marketDataProviders.getFirst();
        MarketDataIngestionRequest request = provider.latestIngestionRequest();
        marketDataIngestionService.ingest(source + "-" + provider.name(), null, request);
    }
}
