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
import java.util.List;
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

    public DramMarketDataSnapshotService(
            PriceSnapshotRepository priceSnapshotRepository,
            FxRateSnapshotRepository fxRateSnapshotRepository,
            DramSnapshotService dramSnapshotService
    ) {
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.fxRateSnapshotRepository = fxRateSnapshotRepository;
        this.dramSnapshotService = dramSnapshotService;
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
        return latestPrices(ticker, exchange).getFirst().getPrice();
    }

    private HoldingInput toHoldingInput(MarketDataHoldingRequest holding) {
        String ticker = normalize(holding.ticker());
        String exchange = normalize(holding.exchange());
        String currency = normalize(holding.currency());
        List<PriceSnapshot> prices = latestPrices(ticker, exchange);
        List<FxRateSnapshot> fxRates = latestFxRates(currency);

        return new HoldingInput(
                ticker,
                holding.name().trim(),
                exchange,
                currency,
                holding.weight(),
                prices.getFirst().getPrice(),
                priorPrice(prices),
                fxRates.getFirst().getRate(),
                priorFxRate(fxRates)
        );
    }

    private List<PriceSnapshot> latestPrices(String ticker, String exchange) {
        List<PriceSnapshot> prices = priceSnapshotRepository
                .findTop2BySecurityTickerAndSecurityExchangeOrderByObservedAtDesc(ticker, exchange);
        if (prices.isEmpty()) {
            throw new IllegalStateException("No price snapshot exists for " + ticker + " on " + exchange);
        }
        return prices;
    }

    private List<FxRateSnapshot> latestFxRates(String currency) {
        if (USD.equals(currency)) {
            return List.of(new FxRateSnapshot(USD, USD, BigDecimal.ONE, "identity", Instant.EPOCH, Instant.EPOCH));
        }

        List<FxRateSnapshot> rates = fxRateSnapshotRepository
                .findTop2ByBaseCurrencyAndQuoteCurrencyOrderByObservedAtDesc(currency, USD);
        if (rates.isEmpty()) {
            throw new IllegalStateException("No FX rate snapshot exists for " + currency + "/" + USD);
        }
        return rates;
    }

    private BigDecimal priorPrice(List<PriceSnapshot> prices) {
        return prices.size() > 1 ? prices.get(1).getPrice() : prices.getFirst().getPrice();
    }

    private BigDecimal priorFxRate(List<FxRateSnapshot> rates) {
        return rates.size() > 1 ? rates.get(1).getRate() : rates.getFirst().getRate();
    }

    private String defaulted(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : normalize(value);
    }

    private String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
