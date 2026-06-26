package com.temadison.drambuilder.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BridgeScoreServiceTest {

    private final BridgeScoreService service = new BridgeScoreService();

    @Test
    void scoresEfficientBridgeAsHoldDram() {
        BridgeScoreResult result = service.calculate(new BridgeScoreInput(
                new BigDecimal("0.45"),
                new BigDecimal("-1.50"),
                new BigDecimal("70"),
                new BigDecimal("65"),
                new BigDecimal("50"),
                false
        ));

        assertThat(result.targetExposureScore()).isEqualByComparingTo("90.00");
        assertThat(result.premiumDiscountScore()).isEqualByComparingTo("100.00");
        assertThat(result.score()).isEqualByComparingTo("84.00");
        assertThat(result.rotationSignal()).isEqualTo(RotationSignal.HOLD_DRAM);
    }

    @Test
    void recommendsRotationWhenDirectHynixIsAvailableAndPremiumIsHigh() {
        BridgeScoreResult result = service.calculate(new BridgeScoreInput(
                new BigDecimal("0.45"),
                new BigDecimal("6.00"),
                new BigDecimal("70"),
                new BigDecimal("65"),
                new BigDecimal("50"),
                true
        ));

        assertThat(result.score()).isEqualByComparingTo("61.50");
        assertThat(result.rotationSignal()).isEqualTo(RotationSignal.ROTATE_TO_SK_HYNIX);
    }

    @Test
    void avoidsAddingWhenPremiumIsTooHighWithoutDirectRotation() {
        BridgeScoreResult result = service.calculate(new BridgeScoreInput(
                new BigDecimal("0.45"),
                new BigDecimal("4.00"),
                new BigDecimal("70"),
                new BigDecimal("65"),
                new BigDecimal("50"),
                false
        ));

        assertThat(result.score()).isEqualByComparingTo("67.75");
        assertThat(result.rotationSignal()).isEqualTo(RotationSignal.AVOID_ADDING);
    }

    @Test
    void clampsPlaceholderScoresToZeroThroughOneHundred() {
        BridgeScoreResult result = service.calculate(new BridgeScoreInput(
                new BigDecimal("0.10"),
                BigDecimal.ZERO,
                new BigDecimal("125"),
                new BigDecimal("-5"),
                new BigDecimal("50"),
                false
        ));

        assertThat(result.liquidityScore()).isEqualByComparingTo("100.00");
        assertThat(result.trackingConfidenceScore()).isEqualByComparingTo("0.00");
        assertThat(result.timingRiskScore()).isEqualByComparingTo("50.00");
    }
}
