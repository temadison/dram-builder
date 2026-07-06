package com.temadison.drambuilder.service;

import com.temadison.drambuilder.config.MarketDataFreshnessProperties;
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
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketDataFreshnessService {

    private final PriceSnapshotRepository priceSnapshotRepository;
    private final long maxAgeHours;
    private final MarketCalendar defaultCalendar;
    private final Map<String, MarketCalendar> exchangeCalendars;
    private final List<RequiredPrice> requiredPrices;
    private final Clock clock;

    @Autowired
    public MarketDataFreshnessService(
            PriceSnapshotRepository priceSnapshotRepository,
            MarketDataFreshnessProperties properties
    ) {
        this(
                priceSnapshotRepository,
                properties.getMaxAgeHours(),
                calendar(properties.getMarketZone(), properties.getExpectedAfterLocalTime(), properties.getMarketHolidays()),
                exchangeCalendars(properties),
                properties.getRequiredPrices(),
                Clock.systemUTC()
        );
    }

    MarketDataFreshnessService(
            PriceSnapshotRepository priceSnapshotRepository,
            long maxAgeHours,
            MarketCalendar defaultCalendar,
            Map<String, MarketCalendar> exchangeCalendars,
            String requiredPrices,
            Clock clock
    ) {
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.maxAgeHours = maxAgeHours;
        this.defaultCalendar = defaultCalendar;
        this.exchangeCalendars = Map.copyOf(exchangeCalendars);
        this.requiredPrices = parseRequiredPrices(requiredPrices);
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MarketDataFreshnessResponse freshness() {
        Instant checkedAt = clock.instant();
        LocalDate expectedAsOfDate = expectedAsOfDate(checkedAt, defaultCalendar);
        Instant staleBefore = expectedAsOfDate.atStartOfDay(defaultCalendar.marketZone()).toInstant();

        List<MarketDataPriceFreshnessResponse> priceStatuses = requiredPrices.stream()
                .map(requiredPrice -> priceStatus(requiredPrice, checkedAt))
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
                defaultCalendar.marketZone().getId(),
                defaultCalendar.expectedAfterLocalTime().toString(),
                priceStatuses
        );
    }

    private MarketDataPriceFreshnessResponse priceStatus(RequiredPrice requiredPrice, Instant checkedAt) {
        MarketCalendar calendar = calendarFor(requiredPrice.exchange());
        LocalDate expectedAsOfDate = expectedAsOfDate(checkedAt, calendar);
        return priceSnapshotRepository.findFirstBySecurityTickerAndSecurityExchangeOrderByObservedAtDesc(
                        requiredPrice.ticker(),
                        requiredPrice.exchange()
                )
                .map(snapshot -> toPriceStatus(requiredPrice, snapshot, expectedAsOfDate, calendar))
                .orElseGet(() -> new MarketDataPriceFreshnessResponse(
                        requiredPrice.ticker(),
                        requiredPrice.exchange(),
                        null,
                        expectedAsOfDate,
                        calendar.marketZone().getId(),
                        calendar.expectedAfterLocalTime().toString(),
                        true,
                        false
                ));
    }

    private MarketDataPriceFreshnessResponse toPriceStatus(
            RequiredPrice requiredPrice,
            PriceSnapshot snapshot,
            LocalDate expectedAsOfDate,
            MarketCalendar calendar
    ) {
        LocalDate observedDate = snapshot.getObservedAt().atZone(ZoneOffset.UTC).toLocalDate();
        return new MarketDataPriceFreshnessResponse(
                requiredPrice.ticker(),
                requiredPrice.exchange(),
                snapshot.getObservedAt(),
                expectedAsOfDate,
                calendar.marketZone().getId(),
                calendar.expectedAfterLocalTime().toString(),
                false,
                observedDate.isBefore(expectedAsOfDate)
        );
    }

    private MarketCalendar calendarFor(String exchange) {
        return exchangeCalendars.getOrDefault(exchange, defaultCalendar);
    }

    private LocalDate expectedAsOfDate(Instant checkedAt, MarketCalendar calendar) {
        LocalDate localDate = checkedAt.atZone(calendar.marketZone()).toLocalDate();
        LocalTime localTime = checkedAt.atZone(calendar.marketZone()).toLocalTime();
        LocalDate candidate = localTime.isBefore(calendar.expectedAfterLocalTime()) ? previousDate(localDate) : localDate;
        return previousMarketDateIfNeeded(candidate, calendar);
    }

    private LocalDate previousDate(LocalDate date) {
        return date.minusDays(1);
    }

    private LocalDate previousMarketDateIfNeeded(LocalDate date, MarketCalendar calendar) {
        LocalDate candidate = date;
        while (isNonMarketDate(candidate, calendar)) {
            candidate = candidate.minusDays(1);
        }
        return candidate;
    }

    private boolean isNonMarketDate(LocalDate date, MarketCalendar calendar) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY
                || calendar.marketHolidays().contains(date);
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

    private static MarketCalendar calendar(String marketZone, String expectedAfterLocalTime, String marketHolidays) {
        return new MarketCalendar(
                ZoneId.of(marketZone),
                LocalTime.parse(expectedAfterLocalTime),
                parseMarketHolidays(marketHolidays)
        );
    }

    private static Map<String, MarketCalendar> exchangeCalendars(MarketDataFreshnessProperties properties) {
        Map<String, MarketCalendar> calendars = new LinkedHashMap<>();
        properties.getExchangeCalendars().forEach((exchange, calendar) -> calendars.put(
                exchange.trim().toUpperCase(Locale.ROOT),
                calendar(
                        defaulted(calendar.getMarketZone(), properties.getMarketZone()),
                        defaulted(calendar.getExpectedAfterLocalTime(), properties.getExpectedAfterLocalTime()),
                        defaulted(calendar.getMarketHolidays(), properties.getMarketHolidays())
                )
        ));
        return calendars;
    }

    private static String defaulted(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private record RequiredPrice(String ticker, String exchange) {
    }

    record MarketCalendar(ZoneId marketZone, LocalTime expectedAfterLocalTime, Set<LocalDate> marketHolidays) {

        MarketCalendar {
            marketHolidays = Set.copyOf(marketHolidays);
        }
    }
}
