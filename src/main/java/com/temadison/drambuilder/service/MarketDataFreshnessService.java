package com.temadison.drambuilder.service;

import com.temadison.drambuilder.domain.PriceSnapshot;
import com.temadison.drambuilder.dto.MarketDataFreshnessResponse;
import com.temadison.drambuilder.dto.MarketDataPriceFreshnessResponse;
import com.temadison.drambuilder.repository.PriceSnapshotRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Set;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketDataFreshnessService {

    private final PriceSnapshotRepository priceSnapshotRepository;
    private final long maxAgeHours;
    private final ZoneId marketZone;
    private final LocalTime expectedAfterLocalTime;
    private final Set<LocalDate> marketHolidays;
    private final List<RequiredPrice> requiredPrices;
    private final Clock clock;

    @Autowired
    public MarketDataFreshnessService(
            PriceSnapshotRepository priceSnapshotRepository,
            @Value("${app.market-data.freshness.max-age-hours:18}") long maxAgeHours,
            @Value("${app.market-data.freshness.market-zone:America/Chicago}") String marketZone,
            @Value("${app.market-data.freshness.expected-after-local-time:17:00}") String expectedAfterLocalTime,
            @Value("${app.market-data.freshness.market-holidays:}") String marketHolidays,
            @Value("${app.market-data.freshness.required-prices:BZX:DRAM,NASDAQ:MU,NASDAQ:SNDK,NASDAQ:WDC,NASDAQ:STX}") String requiredPrices
    ) {
        this(
                priceSnapshotRepository,
                maxAgeHours,
                ZoneId.of(marketZone),
                LocalTime.parse(expectedAfterLocalTime),
                parseMarketHolidays(marketHolidays),
                requiredPrices,
                Clock.systemUTC()
        );
    }

    MarketDataFreshnessService(
            PriceSnapshotRepository priceSnapshotRepository,
            long maxAgeHours,
            ZoneId marketZone,
            LocalTime expectedAfterLocalTime,
            Set<LocalDate> marketHolidays,
            String requiredPrices,
            Clock clock
    ) {
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.maxAgeHours = maxAgeHours;
        this.marketZone = marketZone;
        this.expectedAfterLocalTime = expectedAfterLocalTime;
        this.marketHolidays = Set.copyOf(marketHolidays);
        this.requiredPrices = parseRequiredPrices(requiredPrices);
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MarketDataFreshnessResponse freshness() {
        Instant checkedAt = clock.instant();
        LocalDate expectedAsOfDate = expectedAsOfDate(checkedAt);
        Instant staleBefore = expectedAsOfDate.atStartOfDay(marketZone).toInstant();

        List<MarketDataPriceFreshnessResponse> priceStatuses = requiredPrices.stream()
                .map(requiredPrice -> priceStatus(requiredPrice, expectedAsOfDate))
                .toList();

        boolean hasMissing = priceStatuses.stream().anyMatch(MarketDataPriceFreshnessResponse::missing);
        boolean hasStale = priceStatuses.stream().anyMatch(MarketDataPriceFreshnessResponse::stale);
        String status = hasMissing ? "MISSING" : hasStale ? "STALE" : "FRESH";

        return new MarketDataFreshnessResponse(
                status,
                checkedAt,
                staleBefore,
                maxAgeHours,
                expectedAsOfDate,
                marketZone.getId(),
                expectedAfterLocalTime.toString(),
                priceStatuses
        );
    }

    private MarketDataPriceFreshnessResponse priceStatus(RequiredPrice requiredPrice, LocalDate expectedAsOfDate) {
        return priceSnapshotRepository.findFirstBySecurityTickerAndSecurityExchangeOrderByObservedAtDesc(
                        requiredPrice.ticker(),
                        requiredPrice.exchange()
                )
                .map(snapshot -> toPriceStatus(requiredPrice, snapshot, expectedAsOfDate))
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
            LocalDate expectedAsOfDate
    ) {
        LocalDate observedDate = snapshot.getObservedAt().atZone(marketZone).toLocalDate();
        return new MarketDataPriceFreshnessResponse(
                requiredPrice.ticker(),
                requiredPrice.exchange(),
                snapshot.getObservedAt(),
                false,
                observedDate.isBefore(expectedAsOfDate)
        );
    }

    private LocalDate expectedAsOfDate(Instant checkedAt) {
        LocalDate localDate = checkedAt.atZone(marketZone).toLocalDate();
        LocalTime localTime = checkedAt.atZone(marketZone).toLocalTime();
        LocalDate candidate = localTime.isBefore(expectedAfterLocalTime) ? previousDate(localDate) : localDate;
        return previousMarketDateIfNeeded(candidate);
    }

    private LocalDate previousDate(LocalDate date) {
        return date.minusDays(1);
    }

    private LocalDate previousMarketDateIfNeeded(LocalDate date) {
        LocalDate candidate = date;
        while (isNonMarketDate(candidate)) {
            candidate = candidate.minusDays(1);
        }
        return candidate;
    }

    private boolean isNonMarketDate(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY
                || marketHolidays.contains(date);
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

    private static Set<LocalDate> parseMarketHolidays(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(entry -> !entry.isBlank())
                .map(LocalDate::parse)
                .collect(Collectors.toUnmodifiableSet());
    }

    private record RequiredPrice(String ticker, String exchange) {
    }
}
