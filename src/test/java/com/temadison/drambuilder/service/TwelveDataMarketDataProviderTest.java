package com.temadison.drambuilder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.temadison.drambuilder.config.TwelveDataProviderProperties;
import com.temadison.drambuilder.dto.MarketDataIngestionRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TwelveDataMarketDataProviderTest {

    @Test
    void mapsConfiguredSymbolsToPriceAndFxIngestionRecords() {
        TwelveDataProviderProperties properties = new TwelveDataProviderProperties();
        properties.setApiKey("test-key");
        Map<String, TwelveDataProviderProperties.Symbol> symbols = new LinkedHashMap<>();
        symbols.put("sk-hynix", symbol("000660", "XKRX", "SK hynix", "KRW"));
        symbols.put("mu", symbol("MU", "NASDAQ", "Micron Technology", "USD"));
        properties.setSymbols(symbols);
        TwelveDataClient client = mock(TwelveDataClient.class);
        Instant current = Instant.parse("2026-06-01T00:00:00Z");
        Instant prior = Instant.parse("2026-05-29T00:00:00Z");
        when(client.dailyCloses("MU", "NASDAQ", 2)).thenReturn(List.of(
                new TwelveDataClient.DailyClose(current, new BigDecimal("103.55")),
                new TwelveDataClient.DailyClose(prior, new BigDecimal("97.10"))
        ));
        when(client.dailyCloses("000660", "XKRX", 2)).thenReturn(List.of(
                new TwelveDataClient.DailyClose(current, new BigDecimal("314000")),
                new TwelveDataClient.DailyClose(prior, new BigDecimal("301000"))
        ));
        when(client.dailyCloses("KRW/USD", null, 2)).thenReturn(List.of(
                new TwelveDataClient.DailyClose(current, new BigDecimal("0.00073400")),
                new TwelveDataClient.DailyClose(prior, new BigDecimal("0.00073600"))
        ));

        MarketDataIngestionRequest request = new TwelveDataMarketDataProvider(properties, client).latestIngestionRequest();

        assertThat(request.prices()).hasSize(4);
        assertThat(request.prices().get(0).ticker()).isEqualTo("000660");
        assertThat(request.prices().get(0).currency()).isEqualTo("KRW");
        assertThat(request.prices().get(0).source()).isEqualTo("twelvedata");
        assertThat(request.prices().get(2).ticker()).isEqualTo("MU");
        assertThat(request.fxRates()).hasSize(2);
        assertThat(request.fxRates().get(0).baseCurrency()).isEqualTo("KRW");
        assertThat(request.fxRates().get(0).quoteCurrency()).isEqualTo("USD");
        assertThat(request.officialNavs()).isEmpty();
        assertThat(request.snapshot()).isNull();
    }

    private TwelveDataProviderProperties.Symbol symbol(String ticker, String exchange, String name, String currency) {
        TwelveDataProviderProperties.Symbol symbol = new TwelveDataProviderProperties.Symbol();
        symbol.setSymbol(ticker);
        symbol.setExchange(exchange);
        symbol.setName(name);
        symbol.setCurrency(currency);
        return symbol;
    }
}
