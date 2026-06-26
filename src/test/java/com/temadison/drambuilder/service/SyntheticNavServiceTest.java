package com.temadison.drambuilder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.temadison.drambuilder.dto.HoldingInput;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class SyntheticNavServiceTest {

    private final SyntheticNavService service = new SyntheticNavService();

    @Test
    void calculatesSyntheticNavFromWeightedUsdReturns() {
        SyntheticNavResult result = service.calculate(
                new BigDecimal("80.00"),
                List.of(
                        holding("000660", "SK hynix", "KRX", "KRW", "0.25", "110000", "100000", "0.00080", "0.00080"),
                        holding("MU", "Micron Technology", "NASDAQ", "USD", "0.20", "105", "100", "1", "1"),
                        holding("005930", "Samsung Electronics", "KRX", "KRW", "0.15", "77600", "80000", "0.00082", "0.00080")
                )
        );

        assertThat(result.estimatedEtfMovePercent()).isEqualByComparingTo("3.413750");
        assertThat(result.syntheticNav()).isEqualByComparingTo("82.731000");
        assertThat(result.premiumDiscountPercent()).isEqualByComparingTo("-3.301060");
    }

    @Test
    void calculatesHoldingLevelLocalFxAndWeightedContribution() {
        SyntheticNavResult result = service.calculate(
                new BigDecimal("80.00"),
                List.of(holding("000660", "SK hynix", "KRX", "KRW", "0.25", "110000", "100000", "0.00082", "0.00080"))
        );

        HoldingCalculation hynix = result.holdings().getFirst();

        assertThat(hynix.localReturnPercent()).isEqualByComparingTo("10.000000");
        assertThat(hynix.fxReturnPercent()).isEqualByComparingTo("2.500000");
        assertThat(hynix.totalReturnPercent()).isEqualByComparingTo("12.750000");
        assertThat(hynix.weightedContributionPercent()).isEqualByComparingTo("3.187500");
    }

    @Test
    void rejectsWeightsAboveOneHundredPercent() {
        assertThatThrownBy(() -> service.calculate(
                new BigDecimal("80.00"),
                List.of(
                        holding("000660", "SK hynix", "KRX", "KRW", "0.70", "110000", "100000", "0.00080", "0.00080"),
                        holding("MU", "Micron Technology", "NASDAQ", "USD", "0.40", "105", "100", "1", "1")
                )
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weights");
    }

    private HoldingInput holding(
            String ticker,
            String name,
            String exchange,
            String currency,
            String weight,
            String currentPrice,
            String priorPrice,
            String currentFxToUsd,
            String priorFxToUsd
    ) {
        return new HoldingInput(
                ticker,
                name,
                exchange,
                currency,
                new BigDecimal(weight),
                new BigDecimal(currentPrice),
                new BigDecimal(priorPrice),
                new BigDecimal(currentFxToUsd),
                new BigDecimal(priorFxToUsd)
        );
    }
}
