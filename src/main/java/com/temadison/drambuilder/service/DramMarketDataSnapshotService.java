package com.temadison.drambuilder.service;

import com.temadison.drambuilder.domain.FxRateSnapshot;
import com.temadison.drambuilder.domain.PriceSnapshot;
import com.temadison.drambuilder.dto.HoldingInput;
import com.temadison.drambuilder.dto.MarketDataHoldingRequest;
import com.temadison.drambuilder.dto.MarketDataSnapshotRequest;
import com.temadison.drambuilder.dto.SnapshotRequest;
import com.temadison.drambuilder.dto.SnapshotResponse;
import com.temadison.drambuilder.repository.FxRateSnapshotRepository;
import com.temadison.drambuilder.repository.PriceSnapshotRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Builds DRAM snapshots from previously stored market data records. This keeps
 * price/FX collection separate from NAV calculation while reusing the same
 * snapshot persistence and synthetic NAV workflow as manual entry.
 */
@Service
public class DramMarketDataSnapshotService {

    private static final String DEFAULT_ETF_TICKER = "DRAM";
    private static final String DEFAULT_ETF_EXCHANGE = "NYSEARCA";
    private static final String USD = "USD";

    private final PriceSnapshotRepository priceSnapshotRepository;
    private final FxRateSnapshotRepository fxRateSnapshotRepository;
    private final DramSnapshotService dramSnapshotService;
    private final SnapshotInputValidator snapshotInputValidator;
    private final MarketDataSnapshotReadinessValidator marketDataSnapshotReadinessValidator;

    public DramMarketDataSnapshotService(
            PriceSnapshotRepository priceSnapshotRepository,
            FxRateSnapshotRepository fxRateSnapshotRepository,
            DramSnapshotService dramSnapshotService,
            SnapshotInputValidator snapshotInputValidator,
            MarketDataSnapshotReadinessValidator marketDataSnapshotReadinessValidator
    ) {
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.fxRateSnapshotRepository = fxRateSnapshotRepository;
        this.dramSnapshotService = dramSnapshotService;
        this.snapshotInputValidator = snapshotInputValidator;
        this.marketDataSnapshotReadinessValidator = marketDataSnapshotReadinessValidator;
    }

    /**
     * Creates a DRAM snapshot using latest persisted market data. A holding's
     * price/FX values from the snapshot date become the current inputs; the
     * previous date's observations become the prior inputs.
     *
     * @param request ETF-level parameters and holding identities/weights
     * @return persisted snapshot response with synthetic NAV and attribution
     */
    public SnapshotResponse createSnapshot(MarketDataSnapshotRequest request) {
        snapshotInputValidator.validate(request);
        LocalDate asOfDate = request.asOfDate() == null ? LocalDate.now() : request.asOfDate();
        marketDataSnapshotReadinessValidator.validateReady(request, asOfDate);

        BigDecimal marketPrice = request.marketPrice() == null
                ? latestEtfPrice(request)
                : request.marketPrice();

        SnapshotRequest snapshotRequest = new SnapshotRequest(
                asOfDate,
                marketPrice,
                request.purchasePrice(),
                request.holdings().stream()
                        .map(holding -> toHoldingInput(holding, asOfDate))
                        .toList()
        );
        return dramSnapshotService.createSnapshot(snapshotRequest);
    }

    private BigDecimal latestEtfPrice(MarketDataSnapshotRequest request) {
        String ticker = defaulted(request.etfTicker(), DEFAULT_ETF_TICKER);
        String exchange = defaulted(request.etfExchange(), DEFAULT_ETF_EXCHANGE);
        LocalDate asOfDate = request.asOfDate() == null ? LocalDate.now() : request.asOfDate();
        return currentPrice(ticker, exchange, asOfDate).getPrice();
    }

    private HoldingInput toHoldingInput(MarketDataHoldingRequest holding, LocalDate asOfDate) {
        String ticker = normalize(holding.ticker());
        String exchange = normalize(holding.exchange());
        String currency = normalize(holding.currency());
        PriceSnapshot currentPrice = currentPrice(ticker, exchange, asOfDate);
        PriceSnapshot priorPrice = priorPrice(ticker, exchange, asOfDate);
        FxRateSnapshot currentFxRate = currentFxRate(currency, asOfDate);
        FxRateSnapshot priorFxRate = priorFxRate(currency, asOfDate);

        return new HoldingInput(
                ticker,
                holding.name().trim(),
                exchange,
                currency,
                holding.weight(),
                currentPrice.getPrice(),
                priorPrice.getPrice(),
                currentFxRate.getRate(),
                priorFxRate.getRate()
        );
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

    private PriceSnapshot priorPrice(String ticker, String exchange, LocalDate asOfDate) {
        return priceSnapshotRepository
                .findFirstBySecurityTickerAndSecurityExchangeAndObservedAtBeforeOrderByObservedAtDesc(
                        ticker,
                        exchange,
                        asOfDate.atStartOfDay(ZoneOffset.UTC).toInstant()
                )
                .orElseThrow(() -> new IllegalStateException("No prior price snapshot exists for " + ticker + " on " + exchange));
    }

    private FxRateSnapshot currentFxRate(String currency, LocalDate asOfDate) {
        if (USD.equals(currency)) {
            return identityFxRate();
        }

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

    private FxRateSnapshot priorFxRate(String currency, LocalDate asOfDate) {
        if (USD.equals(currency)) {
            return identityFxRate();
        }

        return fxRateSnapshotRepository
                .findFirstByBaseCurrencyAndQuoteCurrencyAndObservedAtBeforeOrderByObservedAtDesc(
                        currency,
                        USD,
                        asOfDate.atStartOfDay(ZoneOffset.UTC).toInstant()
                )
                .orElseThrow(() -> new IllegalStateException("No prior FX rate snapshot exists for " + currency + "/" + USD));
    }

    private FxRateSnapshot identityFxRate() {
        return new FxRateSnapshot(USD, USD, BigDecimal.ONE, "identity", Instant.EPOCH, Instant.EPOCH);
    }

    private String defaulted(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : normalize(value);
    }

    private String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
