package com.temadison.drambuilder.service;

import com.temadison.drambuilder.config.TwelveDataProviderProperties;
import com.temadison.drambuilder.dto.FxRateSnapshotRequest;
import com.temadison.drambuilder.dto.MarketDataIngestionRequest;
import com.temadison.drambuilder.dto.PriceSnapshotRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.provider.twelvedata", name = "enabled", havingValue = "true")
public class TwelveDataMarketDataProvider implements MarketDataProvider {

    private static final String SOURCE = "twelvedata";

    private final TwelveDataProviderProperties properties;
    private final TwelveDataClient twelveDataClient;

    public TwelveDataMarketDataProvider(TwelveDataProviderProperties properties, TwelveDataClient twelveDataClient) {
        this.properties = properties;
        this.twelveDataClient = twelveDataClient;
    }

    @Override
    public String name() {
        return "twelvedata";
    }

    @Override
    public MarketDataIngestionRequest latestIngestionRequest() {
        validateConfigured();

        List<PriceSnapshotRequest> prices = new ArrayList<>();
        Set<String> fxCurrencies = new LinkedHashSet<>();

        for (TwelveDataProviderProperties.Symbol symbol : properties.getSymbols().values()) {
            List<TwelveDataClient.DailyClose> closes = twelveDataClient.dailyCloses(
                    symbol.getSymbol(),
                    symbol.getExchange(),
                    2
            );
            if (closes.size() < 2) {
                throw new IllegalStateException("Twelve Data returned fewer than two daily closes for " + symbol.getSymbol());
            }

            prices.add(toPriceRequest(symbol, closes.get(0)));
            prices.add(toPriceRequest(symbol, closes.get(1)));

            String currency = normalize(symbol.getCurrency());
            if (!"USD".equals(currency)) {
                fxCurrencies.add(currency);
            }
        }

        List<FxRateSnapshotRequest> fxRates = new ArrayList<>();
        for (String currency : fxCurrencies) {
            List<TwelveDataClient.DailyClose> closes = usdFxCloses(currency);
            if (closes.size() < 2) {
                throw new IllegalStateException("Twelve Data returned fewer than two daily FX closes for " + currency);
            }
            fxRates.add(toFxRequest(currency, closes.get(0)));
            fxRates.add(toFxRequest(currency, closes.get(1)));
        }

        return new MarketDataIngestionRequest(prices, fxRates, List.of(), null);
    }

    private void validateConfigured() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("Twelve Data API key is required when app.provider.twelvedata.enabled=true");
        }
        if (properties.getSymbols().isEmpty()) {
            throw new IllegalStateException("At least one Twelve Data symbol mapping is required");
        }
    }

    private PriceSnapshotRequest toPriceRequest(
            TwelveDataProviderProperties.Symbol symbol,
            TwelveDataClient.DailyClose close
    ) {
        return new PriceSnapshotRequest(
                symbol.getSymbol(),
                symbol.getName(),
                symbol.getExchange(),
                normalize(symbol.getCurrency()),
                close.close(),
                SOURCE,
                close.observedAt()
        );
    }

    private List<TwelveDataClient.DailyClose> usdFxCloses(String currency) {
        try {
            return twelveDataClient.dailyCloses(currency + "/USD", null, 2);
        } catch (IllegalStateException directException) {
            try {
                return twelveDataClient.dailyCloses("USD/" + currency, null, 2).stream()
                        .map(this::invertClose)
                        .toList();
            } catch (IllegalStateException inverseException) {
                throw new IllegalStateException(
                        "Twelve Data returned no usable FX closes for " + currency + "/USD or USD/" + currency,
                        inverseException
                );
            }
        }
    }

    private TwelveDataClient.DailyClose invertClose(TwelveDataClient.DailyClose close) {
        if (BigDecimal.ZERO.compareTo(close.close()) == 0) {
            throw new IllegalStateException("Cannot invert zero FX close");
        }
        return new TwelveDataClient.DailyClose(
                close.observedAt(),
                BigDecimal.ONE.divide(close.close(), 12, RoundingMode.HALF_UP)
        );
    }

    private FxRateSnapshotRequest toFxRequest(String currency, TwelveDataClient.DailyClose close) {
        return new FxRateSnapshotRequest(
                currency,
                "USD",
                close.close(),
                SOURCE,
                close.observedAt()
        );
    }

    private String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
