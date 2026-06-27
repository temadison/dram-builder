package com.temadison.drambuilder.service;

import com.temadison.drambuilder.domain.FxRateSnapshot;
import com.temadison.drambuilder.domain.Etf;
import com.temadison.drambuilder.domain.OfficialNavSnapshot;
import com.temadison.drambuilder.domain.PriceSnapshot;
import com.temadison.drambuilder.domain.Security;
import com.temadison.drambuilder.dto.BulkMarketDataImportRequest;
import com.temadison.drambuilder.dto.BulkMarketDataImportResponse;
import com.temadison.drambuilder.dto.FxRateSnapshotRequest;
import com.temadison.drambuilder.dto.FxRateSnapshotResponse;
import com.temadison.drambuilder.dto.MarketDataSummaryResponse;
import com.temadison.drambuilder.dto.OfficialNavSnapshotRequest;
import com.temadison.drambuilder.dto.OfficialNavSnapshotResponse;
import com.temadison.drambuilder.dto.PriceSnapshotRequest;
import com.temadison.drambuilder.dto.PriceSnapshotResponse;
import com.temadison.drambuilder.repository.EtfRepository;
import com.temadison.drambuilder.repository.FxRateSnapshotRepository;
import com.temadison.drambuilder.repository.OfficialNavSnapshotRepository;
import com.temadison.drambuilder.repository.PriceSnapshotRepository;
import com.temadison.drambuilder.repository.SecurityRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores manually supplied market data with explicit source and observation
 * timestamps. Automated provider ingestion can later use the same service
 * boundary or provide another implementation behind this contract.
 */
@Service
public class MarketDataService {

    private final SecurityRepository securityRepository;
    private final EtfRepository etfRepository;
    private final PriceSnapshotRepository priceSnapshotRepository;
    private final FxRateSnapshotRepository fxRateSnapshotRepository;
    private final OfficialNavSnapshotRepository officialNavSnapshotRepository;

    public MarketDataService(
            SecurityRepository securityRepository,
            EtfRepository etfRepository,
            PriceSnapshotRepository priceSnapshotRepository,
            FxRateSnapshotRepository fxRateSnapshotRepository,
            OfficialNavSnapshotRepository officialNavSnapshotRepository
    ) {
        this.securityRepository = securityRepository;
        this.etfRepository = etfRepository;
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.fxRateSnapshotRepository = fxRateSnapshotRepository;
        this.officialNavSnapshotRepository = officialNavSnapshotRepository;
    }

    @Transactional
    public PriceSnapshotResponse createPriceSnapshot(PriceSnapshotRequest request) {
        Security security = securityRepository.findByTickerAndExchange(normalizeTicker(request.ticker()), normalizeExchange(request.exchange()))
                .orElseGet(() -> securityRepository.save(new Security(
                        normalizeTicker(request.ticker()),
                        request.name().trim(),
                        normalizeExchange(request.exchange()),
                        normalizeCurrency(request.currency())
                )));
        Instant now = Instant.now();
        PriceSnapshot saved = priceSnapshotRepository.save(new PriceSnapshot(
                security,
                request.price(),
                normalizeCurrency(request.currency()),
                request.source().trim(),
                request.observedAt() == null ? now : request.observedAt(),
                now
        ));
        return toPriceResponse(saved);
    }

    @Transactional
    public FxRateSnapshotResponse createFxRateSnapshot(FxRateSnapshotRequest request) {
        Instant now = Instant.now();
        FxRateSnapshot saved = fxRateSnapshotRepository.save(new FxRateSnapshot(
                normalizeCurrency(request.baseCurrency()),
                normalizeCurrency(request.quoteCurrency()),
                request.rate(),
                request.source().trim(),
                request.observedAt() == null ? now : request.observedAt(),
                now
        ));
        return toFxResponse(saved);
    }

    @Transactional
    public OfficialNavSnapshotResponse createOfficialNavSnapshot(OfficialNavSnapshotRequest request) {
        Etf etf = etfRepository.findByTicker(normalizeTicker(request.ticker()))
                .orElseGet(() -> etfRepository.save(new Etf(
                        normalizeTicker(request.ticker()),
                        request.name().trim()
                )));
        Instant now = Instant.now();
        OfficialNavSnapshot saved = officialNavSnapshotRepository.save(new OfficialNavSnapshot(
                etf,
                request.nav(),
                normalizeCurrency(request.currency()),
                request.source().trim(),
                request.asOfDate(),
                request.observedAt() == null ? now : request.observedAt(),
                now
        ));
        return toOfficialNavResponse(saved);
    }

    /**
     * Imports a batch of price and FX snapshots as one transaction. This is
     * intended for repeatable local setup, CSV-adapter output, and future
     * provider ingestion jobs that can produce normalized request records.
     *
     * @param request price and FX snapshots to store
     * @return counts and saved records from the import
     */
    @Transactional
    public BulkMarketDataImportResponse importMarketData(BulkMarketDataImportRequest request) {
        List<PriceSnapshotRequest> prices = request.prices() == null ? List.of() : request.prices();
        List<FxRateSnapshotRequest> fxRates = request.fxRates() == null ? List.of() : request.fxRates();

        if (prices.isEmpty() && fxRates.isEmpty()) {
            throw new IllegalArgumentException("At least one price or FX rate snapshot is required");
        }

        List<PriceSnapshotResponse> importedPrices = prices.stream()
                .map(this::createPriceSnapshot)
                .toList();
        List<FxRateSnapshotResponse> importedFxRates = fxRates.stream()
                .map(this::createFxRateSnapshot)
                .toList();

        return new BulkMarketDataImportResponse(
                importedPrices.size(),
                importedFxRates.size(),
                importedPrices,
                importedFxRates
        );
    }

    @Transactional(readOnly = true)
    public PriceSnapshotResponse latestPrice(String ticker, String exchange) {
        return priceSnapshotRepository.findFirstBySecurityTickerAndSecurityExchangeOrderByObservedAtDesc(
                        normalizeTicker(ticker),
                        normalizeExchange(exchange)
                )
                .map(this::toPriceResponse)
                .orElseThrow(() -> new IllegalStateException("No price snapshot exists for " + ticker + " on " + exchange));
    }

    @Transactional(readOnly = true)
    public FxRateSnapshotResponse latestFxRate(String baseCurrency, String quoteCurrency) {
        return fxRateSnapshotRepository.findFirstByBaseCurrencyAndQuoteCurrencyOrderByObservedAtDesc(
                        normalizeCurrency(baseCurrency),
                        normalizeCurrency(quoteCurrency)
                )
                .map(this::toFxResponse)
                .orElseThrow(() -> new IllegalStateException("No FX rate snapshot exists for " + baseCurrency + "/" + quoteCurrency));
    }

    @Transactional(readOnly = true)
    public OfficialNavSnapshotResponse latestOfficialNav(String ticker) {
        return officialNavSnapshotRepository.findFirstByEtfTickerOrderByObservedAtDesc(normalizeTicker(ticker))
                .map(this::toOfficialNavResponse)
                .orElseThrow(() -> new IllegalStateException("No official NAV snapshot exists for " + ticker));
    }

    @Transactional(readOnly = true)
    public MarketDataSummaryResponse summary() {
        return new MarketDataSummaryResponse(
                priceSnapshotRepository.findTop20ByOrderByObservedAtDesc().stream()
                        .map(this::toPriceResponse)
                        .toList(),
                fxRateSnapshotRepository.findTop20ByOrderByObservedAtDesc().stream()
                        .map(this::toFxResponse)
                        .toList(),
                officialNavSnapshotRepository.findTop20ByOrderByObservedAtDesc().stream()
                        .map(this::toOfficialNavResponse)
                        .toList()
        );
    }

    private PriceSnapshotResponse toPriceResponse(PriceSnapshot snapshot) {
        Security security = snapshot.getSecurity();
        return new PriceSnapshotResponse(
                snapshot.getId(),
                security.getTicker(),
                security.getName(),
                security.getExchange(),
                snapshot.getCurrency(),
                snapshot.getPrice(),
                snapshot.getSource(),
                snapshot.getObservedAt(),
                snapshot.getCreatedAt()
        );
    }

    private FxRateSnapshotResponse toFxResponse(FxRateSnapshot snapshot) {
        return new FxRateSnapshotResponse(
                snapshot.getId(),
                snapshot.getBaseCurrency(),
                snapshot.getQuoteCurrency(),
                snapshot.getRate(),
                snapshot.getSource(),
                snapshot.getObservedAt(),
                snapshot.getCreatedAt()
        );
    }

    private OfficialNavSnapshotResponse toOfficialNavResponse(OfficialNavSnapshot snapshot) {
        Etf etf = snapshot.getEtf();
        return new OfficialNavSnapshotResponse(
                snapshot.getId(),
                etf.getTicker(),
                etf.getName(),
                snapshot.getNav(),
                snapshot.getCurrency(),
                snapshot.getSource(),
                snapshot.getAsOfDate(),
                snapshot.getObservedAt(),
                snapshot.getCreatedAt()
        );
    }

    private String normalizeTicker(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeExchange(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCurrency(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
