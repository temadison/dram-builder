package com.temadison.drambuilder.service;

import com.temadison.drambuilder.domain.FxRateSnapshot;
import com.temadison.drambuilder.domain.PriceSnapshot;
import com.temadison.drambuilder.dto.MarketDataHoldingRequest;
import com.temadison.drambuilder.dto.MarketDataSnapshotRequest;
import com.temadison.drambuilder.repository.FxRateSnapshotRepository;
import com.temadison.drambuilder.repository.OfficialNavSnapshotRepository;
import com.temadison.drambuilder.repository.PriceSnapshotRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class MarketDataSnapshotReadinessValidator {

    private static final String DEFAULT_ETF_TICKER = "DRAM";
    private static final String DEFAULT_ETF_EXCHANGE = "NYSEARCA";
    private static final String USD = "USD";

    private final PriceSnapshotRepository priceSnapshotRepository;
    private final FxRateSnapshotRepository fxRateSnapshotRepository;
    private final OfficialNavSnapshotRepository officialNavSnapshotRepository;

    public MarketDataSnapshotReadinessValidator(
            PriceSnapshotRepository priceSnapshotRepository,
            FxRateSnapshotRepository fxRateSnapshotRepository,
            OfficialNavSnapshotRepository officialNavSnapshotRepository
    ) {
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.fxRateSnapshotRepository = fxRateSnapshotRepository;
        this.officialNavSnapshotRepository = officialNavSnapshotRepository;
    }

    public void validateReady(MarketDataSnapshotRequest request, LocalDate asOfDate) {
        String etfTicker = defaulted(request.etfTicker(), DEFAULT_ETF_TICKER);
        String etfExchange = defaulted(request.etfExchange(), DEFAULT_ETF_EXCHANGE);

        if (request.marketPrice() == null) {
            currentPrice(etfTicker, etfExchange, asOfDate);
        }

        requireOfficialNav(etfTicker, asOfDate);
        request.holdings().forEach(holding -> validateHolding(holding, asOfDate));
    }

    private void validateHolding(MarketDataHoldingRequest holding, LocalDate asOfDate) {
        String ticker = normalize(holding.ticker());
        String exchange = normalize(holding.exchange());
        String currency = normalize(holding.currency());

        currentPrice(ticker, exchange, asOfDate);
        requirePriorPrice(ticker, exchange, asOfDate);

        if (!USD.equals(currency)) {
            currentFxRate(currency, asOfDate);
            requirePriorFx(currency, asOfDate);
        }
    }

    private PriceSnapshot currentPrice(String ticker, String exchange, LocalDate asOfDate) {
        Instant start = asOfDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = asOfDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return priceSnapshotRepository
                .findFirstBySecurityTickerAndSecurityExchangeAndObservedAtGreaterThanEqualAndObservedAtBeforeOrderByObservedAtDesc(
                        ticker,
                        exchange,
                        start,
                        end
                )
                .orElseThrow(() -> new IllegalStateException("No price snapshot exists for " + ticker + " on " + exchange + " as of " + asOfDate));
    }

    private void requirePriorPrice(String ticker, String exchange, LocalDate asOfDate) {
        priceSnapshotRepository.findFirstBySecurityTickerAndSecurityExchangeAndObservedAtBeforeOrderByObservedAtDesc(
                        ticker,
                        exchange,
                        asOfDate.atStartOfDay(ZoneOffset.UTC).toInstant()
                )
                .orElseThrow(() -> new IllegalStateException("No prior price snapshot exists for " + ticker + " on " + exchange));
    }

    private FxRateSnapshot currentFxRate(String currency, LocalDate asOfDate) {
        Instant start = asOfDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = asOfDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return fxRateSnapshotRepository
                .findFirstByBaseCurrencyAndQuoteCurrencyAndObservedAtGreaterThanEqualAndObservedAtBeforeOrderByObservedAtDesc(
                        currency,
                        USD,
                        start,
                        end
                )
                .orElseThrow(() -> new IllegalStateException("No FX rate snapshot exists for " + currency + "/" + USD + " as of " + asOfDate));
    }

    private void requirePriorFx(String currency, LocalDate asOfDate) {
        fxRateSnapshotRepository.findFirstByBaseCurrencyAndQuoteCurrencyAndObservedAtBeforeOrderByObservedAtDesc(
                        currency,
                        USD,
                        asOfDate.atStartOfDay(ZoneOffset.UTC).toInstant()
                )
                .orElseThrow(() -> new IllegalStateException("No prior FX rate snapshot exists for " + currency + "/" + USD));
    }

    private void requireOfficialNav(String etfTicker, LocalDate asOfDate) {
        officialNavSnapshotRepository.findFirstByEtfTickerAndAsOfDateOrderByObservedAtDesc(etfTicker, asOfDate)
                .orElseThrow(() -> new IllegalStateException("No official NAV snapshot exists for " + etfTicker + " as of " + asOfDate));
    }

    private String defaulted(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : normalize(value);
    }

    private String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
