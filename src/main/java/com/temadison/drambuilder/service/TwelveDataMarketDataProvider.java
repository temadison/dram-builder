package com.temadison.drambuilder.service;

import com.temadison.drambuilder.config.TwelveDataProviderProperties;
import com.temadison.drambuilder.dto.MarketDataIngestionRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.provider.twelvedata", name = "enabled", havingValue = "true")
public class TwelveDataMarketDataProvider implements MarketDataProvider {

    private final TwelveDataProviderProperties properties;

    public TwelveDataMarketDataProvider(TwelveDataProviderProperties properties) {
        this.properties = properties;
    }

    @Override
    public String name() {
        return "twelvedata";
    }

    @Override
    public MarketDataIngestionRequest latestIngestionRequest() {
        validateConfigured();
        throw new IllegalStateException("Twelve Data HTTP ingestion is not implemented yet");
    }

    private void validateConfigured() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("Twelve Data API key is required when app.provider.twelvedata.enabled=true");
        }
        if (properties.getSymbols().isEmpty()) {
            throw new IllegalStateException("At least one Twelve Data symbol mapping is required");
        }
    }
}
