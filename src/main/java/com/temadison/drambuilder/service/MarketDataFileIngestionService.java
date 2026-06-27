package com.temadison.drambuilder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temadison.drambuilder.dto.MarketDataIngestionRequest;
import java.io.InputStream;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
public class MarketDataFileIngestionService {

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final MarketDataIngestionService marketDataIngestionService;

    public MarketDataFileIngestionService(
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader,
            MarketDataIngestionService marketDataIngestionService
    ) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.marketDataIngestionService = marketDataIngestionService;
    }

    public void ingestFile(String source, String ingestionFile) throws Exception {
        MarketDataIngestionRequest request;
        try {
            if (ingestionFile == null || ingestionFile.isBlank()) {
                throw new IllegalArgumentException("app.ingest.file is required");
            }

            Resource resource = resourceLoader.getResource(ingestionFile);
            if (!resource.exists()) {
                throw new IllegalArgumentException("Ingestion file does not exist: " + ingestionFile);
            }

            try (InputStream inputStream = resource.getInputStream()) {
                request = objectMapper.readValue(inputStream, MarketDataIngestionRequest.class);
            }
        } catch (Exception exception) {
            marketDataIngestionService.recordFailure(source, ingestionFile, exception);
            throw exception;
        }

        marketDataIngestionService.ingest(source, ingestionFile, request);
    }
}
