package com.temadison.drambuilder.service;

import com.temadison.drambuilder.dto.MarketDataIngestionRequest;

public interface MarketDataProvider {

    String name();

    MarketDataIngestionRequest latestIngestionRequest();
}
