package com.temadison.drambuilder.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class AttributionServiceTest {

    private final AttributionService service = new AttributionService();

    @Test
    void returnsNoPriorSnapshotResultWhenPriorIsMissing() {
        AttributionResult result = service.calculate(
                new AttributionSnapshotInput(
                        2L,
                        new BigDecimal("80.00"),
                        new BigDecimal("82.00"),
                        List.of(holding("MU", "Micron Technology", "0.20", "1.000000"))
                ),
                null
        );

        assertThat(result.hasPriorSnapshot()).isFalse();
        assertThat(result.currentSnapshotId()).isEqualTo(2L);
        assertThat(result.priorSnapshotId()).isNull();
        assertThat(result.syntheticNavChangePercent()).isNull();
        assertThat(result.marketPriceChangePercent()).isNull();
        assertThat(result.topContributors()).isEmpty();
    }

    @Test
    void calculatesSnapshotChangesAndRanksTopContributorsByAbsoluteContributionChange() {
        AttributionResult result = service.calculate(
                new AttributionSnapshotInput(
                        2L,
                        new BigDecimal("81.00"),
                        new BigDecimal("84.00"),
                        List.of(
                                holding("000660", "SK hynix", "0.25", "2.500000"),
                                holding("MU", "Micron Technology", "0.20", "1.000000"),
                                holding("005930", "Samsung Electronics", "0.15", "-0.450000")
                        )
                ),
                new AttributionSnapshotInput(
                        1L,
                        new BigDecimal("80.00"),
                        new BigDecimal("82.00"),
                        List.of(
                                holding("000660", "SK hynix", "0.24", "1.100000"),
                                holding("MU", "Micron Technology", "0.20", "1.500000"),
                                holding("005930", "Samsung Electronics", "0.15", "0.100000")
                        )
                )
        );

        assertThat(result.hasPriorSnapshot()).isTrue();
        assertThat(result.currentSnapshotId()).isEqualTo(2L);
        assertThat(result.priorSnapshotId()).isEqualTo(1L);
        assertThat(result.syntheticNavChangePercent()).isEqualByComparingTo("2.439024");
        assertThat(result.marketPriceChangePercent()).isEqualByComparingTo("1.250000");

        assertThat(result.topContributors()).extracting(HoldingAttribution::ticker)
                .containsExactly("000660", "005930", "MU");
        assertThat(result.topContributors().getFirst().contributionChangePercent()).isEqualByComparingTo("1.400000");
    }

    @Test
    void treatsNewHoldingAsZeroPriorContribution() {
        AttributionResult result = service.calculate(
                new AttributionSnapshotInput(
                        2L,
                        new BigDecimal("81.00"),
                        new BigDecimal("84.00"),
                        List.of(holding("NEW", "New Holding", "0.10", "0.700000"))
                ),
                new AttributionSnapshotInput(
                        1L,
                        new BigDecimal("80.00"),
                        new BigDecimal("82.00"),
                        List.of()
                )
        );

        HoldingAttribution attribution = result.topContributors().getFirst();
        assertThat(attribution.priorWeight()).isEqualByComparingTo("0.000000");
        assertThat(attribution.priorContributionPercent()).isEqualByComparingTo("0.000000");
        assertThat(attribution.contributionChangePercent()).isEqualByComparingTo("0.700000");
    }

    private HoldingCalculation holding(String ticker, String name, String weight, String contribution) {
        return new HoldingCalculation(
                ticker,
                name,
                "TEST",
                "USD",
                new BigDecimal(weight),
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal(contribution)
        );
    }
}
