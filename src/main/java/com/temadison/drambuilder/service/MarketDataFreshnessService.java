package com.temadison.drambuilder.service;

import com.temadison.drambuilder.domain.PriceSnapshot;
import com.temadison.drambuilder.dto.MarketDataFreshnessResponse;
import com.temadison.drambuilder.dto.MarketDataPriceFreshnessResponse;
import com.temadison.drambuilder.repository.PriceSnapshotRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketDataFreshnessService {

    private final PriceSnapshotRepository priceSnapshotRepository;
    private final long maxAgeHours;
    private final List<RequiredPrice> requiredPrices;

    public MarketDataFreshnessService(
            PriceSnapshotRepository priceSnapshotRepository,
            @Value("${app.market-data.freshness.max-age-hours:18}") long maxAgeHours,
            @Value("${app.market-data.freshness.required-prices:BATS:DRAM,NASDAQ:MU,NASDAQ:SNDK,NASDAQ:WDC,NASDAQ:STX}") String requiredPrices
    ) {
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.maxAgeHours = maxAgeHours;
        this.requiredPrices = parseRequiredPrices(requiredPrices);
    }

    @Transactional(readOnly = true)
    public MarketDataFreshnessResponse freshness() {
        Instant checkedAt = Instant.now();
        Instant staleBefore = checkedAt.minus(Duration.ofHours(maxAgeHours));

        List<MarketDataPriceFreshnessResponse> priceStatuses = requiredPrices.stream()
                .map(requiredPrice -> priceStatus(requiredPrice, staleBefore))
                .toList();

        boolean hasMissing = priceStatuses.stream().anyMatch(MarketDataPriceFreshnessResponse::missing);
        boolean hasStale = priceStatuses.stream().anyMatch(MarketDataPriceFreshnessResponse::stale);
        String status = hasMissing ? "MISSING" : hasStale ? "STALE" : "FRESH";

        return new MarketDataFreshnessResponse(status, checkedAt, staleBefore, maxAgeHours, priceStatuses);
    }

    private MarketDataPriceFreshnessResponse priceStatus(RequiredPrice requiredPrice, Instant staleBefore) {
        return priceSnapshotRepository.findFirstBySecurityTickerAndSecurityExchangeOrderByObservedAtDesc(
                        requiredPrice.ticker(),
                        requiredPrice.exchange()
                )
                .map(snapshot -> toPriceStatus(requiredPrice, snapshot, staleBefore))
                .orElseGet(() -> new MarketDataPriceFreshnessResponse(
                        requiredPrice.ticker(),
                        requiredPrice.exchange(),
                        null,
                        true,
                        false
                ));
    }

    private MarketDataPriceFreshnessResponse toPriceStatus(
            RequiredPrice requiredPrice,
            PriceSnapshot snapshot,
            Instant staleBefore
    ) {
        return new MarketDataPriceFreshnessResponse(
                requiredPrice.ticker(),
                requiredPrice.exchange(),
                snapshot.getObservedAt(),
                false,
                snapshot.getObservedAt().isBefore(staleBefore)
        );
    }

    private List<RequiredPrice> parseRequiredPrices(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(entry -> !entry.isBlank())
                .map(this::parseRequiredPrice)
                .toList();
    }

    private RequiredPrice parseRequiredPrice(String value) {
        String[] parts = value.split(":");
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalArgumentException("Invalid required price entry: " + value);
        }
        return new RequiredPrice(
                parts[1].trim().toUpperCase(Locale.ROOT),
                parts[0].trim().toUpperCase(Locale.ROOT)
        );
    }

    private record RequiredPrice(String ticker, String exchange) {
    }
}
