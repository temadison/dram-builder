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

    public DramMarketDataSnapshotService(
            PriceSnapshotRepository priceSnapshotRepository,
            FxRateSnapshotRepository fxRateSnapshotRepository,
            DramSnapshotService dramSnapshotService,
            SnapshotInputValidator snapshotInputValidator
    ) {
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.fxRateSnapshotRepository = fxRateSnapshotRepository;
        this.dramSnapshotService = dramSnapshotService;
        this.snapshotInputValidator = snapshotInputValidator;
    }

    /**
     * Creates a DRAM snapshot using latest persisted market data. A holding's
     * most recent price/FX values become the current inputs; the previous
     * observations become the prior inputs. If only one observation exists, it
     * is reused for prior inputs so the snapshot remains usable.
     *
     * @param request ETF-level parameters and holding identities/weights
     * @return persisted snapshot response with synthetic NAV and attribution
     */
    public SnapshotResponse createSnapshot(MarketDataSnapshotRequest request) {
        snapshotInputValidator.validate(request);

        BigDecimal marketPrice = request.marketPrice() == null
                ? latestEtfPrice(request)
                : request.marketPrice();

        SnapshotRequest snapshotRequest = new SnapshotRequest(
                request.asOfDate() == null ? LocalDate.now() : request.asOfDate(),
                marketPrice,
                request.purchasePrice(),
                request.holdings().stream()
                        .map(this::toHoldingInput)
                        .toList()
        );
        return dramSnapshotService.createSnapshot(snapshotRequest);
    }

    private BigDecimal latestEtfPrice(MarketDataSnapshotRequest request) {
        String ticker = defaulted(request.etfTicker(), DEFAULT_ETF_TICKER);
        String exchange = defaulted(request.etfExchange(), DEFAULT_ETF_EXCHANGE);
        return currentPrice(ticker, exchange).getPrice();
    }

    private HoldingInput toHoldingInput(MarketDataHoldingRequest holding) {
        String ticker = normalize(holding.ticker());
        String exchange = normalize(holding.exchange());
        String currency = normalize(holding.currency());
        PriceSnapshot currentPrice = currentPrice(ticker, exchange);
        PriceSnapshot priorPrice = priorPrice(ticker, exchange, currentPrice);
        FxRateSnapshot currentFxRate = currentFxRate(currency);
        FxRateSnapshot priorFxRate = priorFxRate(currency, currentFxRate);

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

    private PriceSnapshot currentPrice(String ticker, String exchange) {
        return priceSnapshotRepository.findFirstBySecurityTickerAndSecurityExchangeOrderByObservedAtDesc(ticker, exchange)
                .orElseThrow(() -> new IllegalStateException("No price snapshot exists for " + ticker + " on " + exchange));
    }

    private PriceSnapshot priorPrice(String ticker, String exchange, PriceSnapshot current) {
        return priceSnapshotRepository
                .findFirstBySecurityTickerAndSecurityExchangeAndObservedAtBeforeOrderByObservedAtDesc(
                        ticker,
                        exchange,
                        current.getObservedAt()
                )
                .orElse(current);
    }

    private FxRateSnapshot currentFxRate(String currency) {
        if (USD.equals(currency)) {
            return identityFxRate();
        }

        return fxRateSnapshotRepository.findFirstByBaseCurrencyAndQuoteCurrencyOrderByObservedAtDesc(currency, USD)
                .orElseThrow(() -> new IllegalStateException("No FX rate snapshot exists for " + currency + "/" + USD));
    }

    private FxRateSnapshot priorFxRate(String currency, FxRateSnapshot current) {
        if (USD.equals(currency)) {
            return identityFxRate();
        }

        return fxRateSnapshotRepository
                .findFirstByBaseCurrencyAndQuoteCurrencyAndObservedAtBeforeOrderByObservedAtDesc(
                        currency,
                        USD,
                        current.getObservedAt()
                )
                .orElse(current);
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
