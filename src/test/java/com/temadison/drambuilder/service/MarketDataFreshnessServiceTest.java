package com.temadison.drambuilder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.temadison.drambuilder.domain.PriceSnapshot;
import com.temadison.drambuilder.domain.Security;
import com.temadison.drambuilder.dto.MarketDataFreshnessResponse;
import com.temadison.drambuilder.repository.PriceSnapshotRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MarketDataFreshnessServiceTest {

    private static final ZoneId MARKET_ZONE = ZoneId.of("America/Chicago");

    @Test
    void treatsPreviousBusinessDateAsFreshBeforeMarketDateRollover() {
        MarketDataFreshnessResponse freshness = freshnessAt("2026-07-02T16:31:00Z");

        assertThat(freshness.status()).isEqualTo("FRESH");
        assertThat(freshness.expectedAsOfDate()).isEqualTo(LocalDate.parse("2026-07-01"));
        assertThat(freshness.requiredPrices().getFirst().stale()).isFalse();
    }

    @Test
    void treatsPreviousBusinessDateAsStaleAfterMarketDateRollover() {
        MarketDataFreshnessResponse freshness = freshnessAt("2026-07-02T22:31:00Z");

        assertThat(freshness.status()).isEqualTo("STALE");
        assertThat(freshness.expectedAsOfDate()).isEqualTo(LocalDate.parse("2026-07-02"));
        assertThat(freshness.requiredPrices().getFirst().stale()).isTrue();
    }

    @Test
    void rollsWeekendExpectationBackToFriday() {
        MarketDataFreshnessResponse freshness = freshnessAt(
                "2026-07-05T18:00:00Z",
                "2026-07-03T20:00:00Z"
        );

        assertThat(freshness.status()).isEqualTo("FRESH");
        assertThat(freshness.expectedAsOfDate()).isEqualTo(LocalDate.parse("2026-07-03"));
        assertThat(freshness.requiredPrices().getFirst().stale()).isFalse();
    }

    @Test
    void rollsConfiguredMarketHolidayBackToPreviousMarketDate() {
        MarketDataFreshnessResponse freshness = freshnessAt(
                "2026-07-03T22:31:00Z",
                "2026-07-02T20:00:00Z",
                Set.of(LocalDate.parse("2026-07-03"))
        );

        assertThat(freshness.status()).isEqualTo("FRESH");
        assertThat(freshness.expectedAsOfDate()).isEqualTo(LocalDate.parse("2026-07-02"));
        assertThat(freshness.requiredPrices().getFirst().stale()).isFalse();
    }

    private MarketDataFreshnessResponse freshnessAt(String checkedAt) {
        return freshnessAt(checkedAt, "2026-07-01T20:00:00Z");
    }

    private MarketDataFreshnessResponse freshnessAt(String checkedAt, String observedAt) {
        return freshnessAt(checkedAt, observedAt, Set.of());
    }

    private MarketDataFreshnessResponse freshnessAt(String checkedAt, String observedAt, Set<LocalDate> marketHolidays) {
        PriceSnapshotRepository repository = mock(PriceSnapshotRepository.class);
        when(repository.findFirstBySecurityTickerAndSecurityExchangeOrderByObservedAtDesc("DRAM", "BZX"))
                .thenReturn(Optional.of(price(observedAt)));

        return new MarketDataFreshnessService(
                repository,
                18,
                MARKET_ZONE,
                LocalTime.parse("17:00"),
                marketHolidays,
                "BZX:DRAM",
                Clock.fixed(Instant.parse(checkedAt), ZoneOffset.UTC)
        ).freshness();
    }

    private PriceSnapshot price(String observedAt) {
        return new PriceSnapshot(
                new Security("DRAM", "Roundhill Memory ETF", "BZX", "USD"),
                new BigDecimal("65.86"),
                "USD",
                "test",
                Instant.parse(observedAt),
                Instant.parse(observedAt)
        );
    }
}
