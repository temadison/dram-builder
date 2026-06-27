package com.temadison.drambuilder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.temadison.drambuilder.config.DramSnapshotProperties;
import com.temadison.drambuilder.dto.MarketDataSnapshotRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConfiguredDramSnapshotRequestFactoryTest {

    @Test
    void returnsNullWhenSnapshotCreationIsDisabled() {
        DramSnapshotProperties properties = new DramSnapshotProperties();
        properties.setEnabled(false);
        properties.setPurchasePrice(new BigDecimal("68.00"));
        properties.setHoldings(List.of(holding("MU", "Micron Technology", "NASDAQ", "USD", "0.2383")));

        MarketDataSnapshotRequest request = new ConfiguredDramSnapshotRequestFactory(properties).snapshotRequestOrNull();

        assertThat(request).isNull();
    }

    @Test
    void mapsEnabledConfigurationToSnapshotRequest() {
        DramSnapshotProperties properties = new DramSnapshotProperties();
        properties.setEnabled(true);
        properties.setPurchasePrice(new BigDecimal("68.00"));
        properties.setEtfTicker("DRAM");
        properties.setEtfExchange("BATS");
        properties.setHoldings(List.of(
                holding("MU", "Micron Technology", "NASDAQ", "USD", "0.2383"),
                holding("SNDK", "SanDisk", "NASDAQ", "USD", "0.0466")
        ));

        MarketDataSnapshotRequest request = new ConfiguredDramSnapshotRequestFactory(properties).snapshotRequestOrNull();

        assertThat(request.asOfDate()).isNull();
        assertThat(request.marketPrice()).isNull();
        assertThat(request.purchasePrice()).isEqualByComparingTo("68.00");
        assertThat(request.etfTicker()).isEqualTo("DRAM");
        assertThat(request.etfExchange()).isEqualTo("BATS");
        assertThat(request.holdings()).hasSize(2);
        assertThat(request.holdings().get(0).ticker()).isEqualTo("MU");
        assertThat(request.holdings().get(0).weight()).isEqualByComparingTo("0.2383");
    }

    @Test
    void rejectsEnabledSnapshotWithoutPurchasePrice() {
        DramSnapshotProperties properties = new DramSnapshotProperties();
        properties.setEnabled(true);
        properties.setHoldings(List.of(holding("MU", "Micron Technology", "NASDAQ", "USD", "0.2383")));

        assertThatThrownBy(() -> new ConfiguredDramSnapshotRequestFactory(properties).snapshotRequestOrNull())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("app.dram.snapshot.purchase-price is required when app.dram.snapshot.enabled=true");
    }

    @Test
    void rejectsEnabledSnapshotWithoutHoldings() {
        DramSnapshotProperties properties = new DramSnapshotProperties();
        properties.setEnabled(true);
        properties.setPurchasePrice(new BigDecimal("68.00"));

        assertThatThrownBy(() -> new ConfiguredDramSnapshotRequestFactory(properties).snapshotRequestOrNull())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("At least one app.dram.snapshot.holdings entry is required when app.dram.snapshot.enabled=true");
    }

    private DramSnapshotProperties.Holding holding(
            String ticker,
            String name,
            String exchange,
            String currency,
            String weight
    ) {
        DramSnapshotProperties.Holding holding = new DramSnapshotProperties.Holding();
        holding.setTicker(ticker);
        holding.setName(name);
        holding.setExchange(exchange);
        holding.setCurrency(currency);
        holding.setWeight(new BigDecimal(weight));
        return holding;
    }
}
