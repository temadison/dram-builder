package com.temadison.drambuilder.service;

import com.temadison.drambuilder.config.DramSnapshotProperties;
import com.temadison.drambuilder.domain.FxRateSnapshot;
import com.temadison.drambuilder.domain.OfficialNavSnapshot;
import com.temadison.drambuilder.domain.PriceSnapshot;
import com.temadison.drambuilder.dto.MarketDataSnapshotReadinessIssueResponse;
import com.temadison.drambuilder.dto.MarketDataSnapshotReadinessResponse;
import com.temadison.drambuilder.repository.FxRateSnapshotRepository;
import com.temadison.drambuilder.repository.OfficialNavSnapshotRepository;
import com.temadison.drambuilder.repository.PriceSnapshotRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketDataSnapshotReadinessService {

    private static final String USD = "USD";

    private final DramSnapshotProperties properties;
    private final PriceSnapshotRepository priceSnapshotRepository;
    private final FxRateSnapshotRepository fxRateSnapshotRepository;
    private final OfficialNavSnapshotRepository officialNavSnapshotRepository;

    public MarketDataSnapshotReadinessService(
            DramSnapshotProperties properties,
            PriceSnapshotRepository priceSnapshotRepository,
            FxRateSnapshotRepository fxRateSnapshotRepository,
            OfficialNavSnapshotRepository officialNavSnapshotRepository
    ) {
        this.properties = properties;
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.fxRateSnapshotRepository = fxRateSnapshotRepository;
        this.officialNavSnapshotRepository = officialNavSnapshotRepository;
    }

    @Transactional(readOnly = true)
    public MarketDataSnapshotReadinessResponse readiness() {
        List<MarketDataSnapshotReadinessIssueResponse> issues = new ArrayList<>();

        if (properties.getPurchasePrice() == null || BigDecimal.ZERO.compareTo(properties.getPurchasePrice()) >= 0) {
            issues.add(issue("CONFIG", "purchasePrice", "Snapshot purchase price is not configured"));
        }
        if (properties.getHoldings().isEmpty()) {
            issues.add(issue("CONFIG", "holdings", "Snapshot holdings are not configured"));
        }

        String etfTicker = normalize(defaulted(properties.getEtfTicker(), "DRAM"));
        String etfExchange = normalize(defaulted(properties.getEtfExchange(), "NYSEARCA"));
        Optional<OfficialNavSnapshot> officialNav = officialNavSnapshotRepository.findFirstByEtfTickerOrderByObservedAtDesc(etfTicker);
        LocalDate asOfDate = officialNav.map(OfficialNavSnapshot::getAsOfDate).orElse(LocalDate.now());
        if (officialNav.isEmpty()) {
            issues.add(issue("OFFICIAL_NAV", etfTicker, "Official NAV is missing"));
        }

        validatePrice(etfTicker, etfExchange, asOfDate, issues);
        properties.getHoldings().forEach(holding -> validateHolding(holding, asOfDate, issues));

        String status = properties.getPurchasePrice() == null || properties.getHoldings().isEmpty()
                ? "NOT_CONFIGURED"
                : issues.isEmpty() ? "READY" : "BLOCKED";
        return new MarketDataSnapshotReadinessResponse(status, asOfDate, officialNav.isPresent(), issues);
    }

    private void validateHolding(
            DramSnapshotProperties.Holding holding,
            LocalDate asOfDate,
            List<MarketDataSnapshotReadinessIssueResponse> issues
    ) {
        String ticker = normalize(holding.getTicker());
        String exchange = normalize(holding.getExchange());
        String currency = normalize(holding.getCurrency());

        validatePrice(ticker, exchange, asOfDate, issues);
        if (!USD.equals(currency)) {
            validateFx(currency, asOfDate, issues);
        }
    }

    private void validatePrice(
            String ticker,
            String exchange,
            LocalDate asOfDate,
            List<MarketDataSnapshotReadinessIssueResponse> issues
    ) {
        Instant start = asOfDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = asOfDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Optional<PriceSnapshot> latest = priceSnapshotRepository
                .findFirstBySecurityTickerAndSecurityExchangeAndObservedAtGreaterThanEqualAndObservedAtBeforeOrderByObservedAtDesc(
                ticker,
                exchange,
                start,
                end
        );
        String key = exchange + ":" + ticker;
        if (latest.isEmpty()) {
            issues.add(issue("PRICE", key, "Price is missing for " + asOfDate));
            return;
        }
        boolean hasPrior = priceSnapshotRepository.findFirstBySecurityTickerAndSecurityExchangeAndObservedAtBeforeOrderByObservedAtDesc(
                ticker,
                exchange,
                asOfDate.atStartOfDay(ZoneOffset.UTC).toInstant()
        ).isPresent();
        if (!hasPrior) {
            issues.add(issue("PRICE", key, "Prior price is missing"));
        }
    }

    private void validateFx(
            String currency,
            LocalDate asOfDate,
            List<MarketDataSnapshotReadinessIssueResponse> issues
    ) {
        Instant start = asOfDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = asOfDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Optional<FxRateSnapshot> latest = fxRateSnapshotRepository
                .findFirstByBaseCurrencyAndQuoteCurrencyAndObservedAtGreaterThanEqualAndObservedAtBeforeOrderByObservedAtDesc(
                        currency,
                        USD,
                        start,
                        end
                );
        String key = currency + "/" + USD;
        if (latest.isEmpty()) {
            issues.add(issue("FX", key, "FX rate is missing for " + asOfDate));
            return;
        }
        boolean hasPrior = fxRateSnapshotRepository.findFirstByBaseCurrencyAndQuoteCurrencyAndObservedAtBeforeOrderByObservedAtDesc(
                currency,
                USD,
                asOfDate.atStartOfDay(ZoneOffset.UTC).toInstant()
        ).isPresent();
        if (!hasPrior) {
            issues.add(issue("FX", key, "Prior FX rate is missing"));
        }
    }

    private MarketDataSnapshotReadinessIssueResponse issue(String category, String key, String message) {
        return new MarketDataSnapshotReadinessIssueResponse(category, key, message);
    }

    private String defaulted(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
