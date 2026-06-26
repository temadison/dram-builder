package com.temadison.drambuilder.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ScenarioServiceTest {

    private final ScenarioService service = new ScenarioService();

    @Test
    void calculatesEstimatedDramMoveAndDollarImpact() {
        ScenarioCalculationResult result = service.calculate(new ScenarioInput(
                new BigDecimal("80.00"),
                new BigDecimal("76.31"),
                Map.of(
                        "000660", new BigDecimal("10"),
                        "MU", new BigDecimal("5"),
                        "005930", new BigDecimal("-3")
                ),
                Map.of("KRW", new BigDecimal("2")),
                List.of(
                        holding("000660", "SK hynix", "KRW", "0.25"),
                        holding("MU", "Micron Technology", "USD", "0.20"),
                        holding("005930", "Samsung Electronics", "KRW", "0.15")
                )
        ));

        assertThat(result.estimatedMovePercent()).isEqualByComparingTo("3.891000");
        assertThat(result.projectedMarketPrice()).isEqualByComparingTo("83.112800");
        assertThat(result.dollarImpactVsPurchasePrice()).isEqualByComparingTo("6.802800");
    }

    @Test
    void appliesCombinedSecurityAndFxMoveAtHoldingLevel() {
        ScenarioCalculationResult result = service.calculate(new ScenarioInput(
                new BigDecimal("80.00"),
                new BigDecimal("76.31"),
                Map.of("000660", new BigDecimal("10")),
                Map.of("KRW", new BigDecimal("2")),
                List.of(holding("000660", "SK hynix", "KRW", "0.25"))
        ));

        ScenarioHoldingImpact hynix = result.holdings().getFirst();

        assertThat(hynix.securityMovePercent()).isEqualByComparingTo("10.000000");
        assertThat(hynix.fxMovePercent()).isEqualByComparingTo("2.000000");
        assertThat(hynix.totalMovePercent()).isEqualByComparingTo("12.200000");
        assertThat(hynix.weightedContributionPercent()).isEqualByComparingTo("3.050000");
    }

    @Test
    void usesZeroMoveForUnspecifiedSecurityAndCurrency() {
        ScenarioCalculationResult result = service.calculate(new ScenarioInput(
                new BigDecimal("80.00"),
                new BigDecimal("76.31"),
                Map.of(),
                Map.of(),
                List.of(holding("ASML", "ASML Holding", "EUR", "0.05"))
        ));

        ScenarioHoldingImpact impact = result.holdings().getFirst();

        assertThat(impact.securityMovePercent()).isEqualByComparingTo("0.000000");
        assertThat(impact.fxMovePercent()).isEqualByComparingTo("0.000000");
        assertThat(impact.weightedContributionPercent()).isEqualByComparingTo("0.000000");
        assertThat(result.estimatedMovePercent()).isEqualByComparingTo("0.000000");
    }

    private ScenarioHoldingInput holding(String ticker, String name, String currency, String weight) {
        return new ScenarioHoldingInput(ticker, name, currency, new BigDecimal(weight));
    }
}
