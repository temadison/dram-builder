package com.temadison.drambuilder.service;

import com.temadison.drambuilder.config.DramSnapshotProperties;
import com.temadison.drambuilder.dto.MarketDataHoldingRequest;
import com.temadison.drambuilder.dto.MarketDataSnapshotRequest;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ConfiguredDramSnapshotRequestFactory {

    private final DramSnapshotProperties properties;

    public ConfiguredDramSnapshotRequestFactory(DramSnapshotProperties properties) {
        this.properties = properties;
    }

    public MarketDataSnapshotRequest snapshotRequestOrNull() {
        if (!properties.isEnabled()) {
            return null;
        }
        validate();
        return new MarketDataSnapshotRequest(
                null,
                null,
                properties.getPurchasePrice(),
                properties.getEtfTicker(),
                properties.getEtfExchange(),
                properties.getHoldings().stream()
                        .map(this::toHoldingRequest)
                        .toList()
        );
    }

    private void validate() {
        if (properties.getPurchasePrice() == null || BigDecimal.ZERO.compareTo(properties.getPurchasePrice()) >= 0) {
            throw new IllegalStateException("app.dram.snapshot.purchase-price is required when app.dram.snapshot.enabled=true");
        }
        if (properties.getHoldings().isEmpty()) {
            throw new IllegalStateException("At least one app.dram.snapshot.holdings entry is required when app.dram.snapshot.enabled=true");
        }
    }

    private MarketDataHoldingRequest toHoldingRequest(DramSnapshotProperties.Holding holding) {
        return new MarketDataHoldingRequest(
                holding.getTicker(),
                holding.getName(),
                holding.getExchange(),
                holding.getCurrency(),
                holding.getWeight()
        );
    }
}
