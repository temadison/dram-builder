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
            PriceSnapshot etfPrice = currentPrice(etfTicker, etfExchange);
            requireFreshPrice(etfTicker, etfExchange, etfPrice, asOfDate);
        }

        requireOfficialNav(etfTicker, asOfDate);
        request.holdings().forEach(holding -> validateHolding(holding, asOfDate));
    }

    private void validateHolding(MarketDataHoldingRequest holding, LocalDate asOfDate) {
        String ticker = normalize(holding.ticker());
        String exchange = normalize(holding.exchange());
        String currency = normalize(holding.currency());

        PriceSnapshot price = currentPrice(ticker, exchange);
        requireFreshPrice(ticker, exchange, price, asOfDate);
        requirePriorPrice(ticker, exchange, price);

        if (!USD.equals(currency)) {
            FxRateSnapshot fxRate = currentFxRate(currency);
            requireFreshFx(currency, fxRate, asOfDate);
            requirePriorFx(currency, fxRate);
        }
    }

    private PriceSnapshot currentPrice(String ticker, String exchange) {
        return priceSnapshotRepository.findFirstBySecurityTickerAndSecurityExchangeOrderByObservedAtDesc(ticker, exchange)
                .orElseThrow(() -> new IllegalStateException("No price snapshot exists for " + ticker + " on " + exchange));
    }

    private void requirePriorPrice(String ticker, String exchange, PriceSnapshot current) {
        priceSnapshotRepository.findFirstBySecurityTickerAndSecurityExchangeAndObservedAtBeforeOrderByObservedAtDesc(
                        ticker,
                        exchange,
                        current.getObservedAt()
                )
                .orElseThrow(() -> new IllegalStateException("No prior price snapshot exists for " + ticker + " on " + exchange));
    }

    private FxRateSnapshot currentFxRate(String currency) {
        return fxRateSnapshotRepository.findFirstByBaseCurrencyAndQuoteCurrencyOrderByObservedAtDesc(currency, USD)
                .orElseThrow(() -> new IllegalStateException("No FX rate snapshot exists for " + currency + "/" + USD));
    }

    private void requirePriorFx(String currency, FxRateSnapshot current) {
        fxRateSnapshotRepository.findFirstByBaseCurrencyAndQuoteCurrencyAndObservedAtBeforeOrderByObservedAtDesc(
                        currency,
                        USD,
                        current.getObservedAt()
                )
                .orElseThrow(() -> new IllegalStateException("No prior FX rate snapshot exists for " + currency + "/" + USD));
    }

    private void requireOfficialNav(String etfTicker, LocalDate asOfDate) {
        officialNavSnapshotRepository.findFirstByEtfTickerOrderByObservedAtDesc(etfTicker)
                .filter(nav -> asOfDate.equals(nav.getAsOfDate()))
                .orElseThrow(() -> new IllegalStateException("No official NAV snapshot exists for " + etfTicker + " as of " + asOfDate));
    }

    private void requireFreshPrice(String ticker, String exchange, PriceSnapshot price, LocalDate asOfDate) {
        if (price.getObservedAt().isBefore(asOfDate.atStartOfDay(ZoneOffset.UTC).toInstant())) {
            throw new IllegalStateException("Latest price snapshot is stale for " + ticker + " on " + exchange + " as of " + asOfDate);
        }
    }

    private void requireFreshFx(String currency, FxRateSnapshot fxRate, LocalDate asOfDate) {
        if (fxRate.getObservedAt().isBefore(asOfDate.atStartOfDay(ZoneOffset.UTC).toInstant())) {
            throw new IllegalStateException("Latest FX rate snapshot is stale for " + currency + "/" + USD + " as of " + asOfDate);
        }
    }

    private String defaulted(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : normalize(value);
    }

    private String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
