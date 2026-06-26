package com.temadison.drambuilder.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

/**
 * Calculates Bridge Score v1 from target exposure, premium/discount, and
 * placeholder quality scores. The score is intentionally transparent and
 * conservative until liquidity, tracking, and event timing inputs are automated.
 */
@Service
public class BridgeScoreService implements BridgeScoreCalculator {

    private static final MathContext MATH_CONTEXT = new MathContext(18, RoundingMode.HALF_UP);
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal TARGET_EXPOSURE_FULL_SCORE = new BigDecimal("0.50");
    private static final BigDecimal EXPOSURE_WEIGHT = new BigDecimal("0.45");
    private static final BigDecimal PREMIUM_WEIGHT = new BigDecimal("0.25");
    private static final BigDecimal LIQUIDITY_WEIGHT = new BigDecimal("0.10");
    private static final BigDecimal TRACKING_WEIGHT = new BigDecimal("0.10");
    private static final BigDecimal TIMING_WEIGHT = new BigDecimal("0.10");

    /**
     * Calculates a 0-100 bridge efficiency score and simple rotation signal.
     *
     * <p>Component weights:
     * target exposure 45%, premium/discount 25%, liquidity 10%,
     * tracking confidence 10%, timing risk 10%.
     *
     * @param input score inputs and placeholders
     * @return score components and recommendation
     */
    @Override
    public BridgeScoreResult calculate(BridgeScoreInput input) {
        BigDecimal targetExposureScore = targetExposureScore(input.targetExposureWeight());
        BigDecimal premiumDiscountScore = premiumDiscountScore(input.premiumDiscountPercent());
        BigDecimal liquidityScore = clampScore(input.liquidityScore());
        BigDecimal trackingConfidenceScore = clampScore(input.trackingConfidenceScore());
        BigDecimal timingRiskScore = clampScore(input.timingRiskScore());

        BigDecimal score = weighted(targetExposureScore, EXPOSURE_WEIGHT)
                .add(weighted(premiumDiscountScore, PREMIUM_WEIGHT), MATH_CONTEXT)
                .add(weighted(liquidityScore, LIQUIDITY_WEIGHT), MATH_CONTEXT)
                .add(weighted(trackingConfidenceScore, TRACKING_WEIGHT), MATH_CONTEXT)
                .add(weighted(timingRiskScore, TIMING_WEIGHT), MATH_CONTEXT)
                .setScale(2, RoundingMode.HALF_UP);

        RotationSignal signal = signal(score, input.premiumDiscountPercent(), input.directSkHynixAvailable());
        return new BridgeScoreResult(
                score,
                targetExposureScore,
                premiumDiscountScore,
                liquidityScore,
                trackingConfidenceScore,
                timingRiskScore,
                signal,
                recommendation(signal, input.directSkHynixAvailable())
        );
    }

    private BigDecimal targetExposureScore(BigDecimal targetExposureWeight) {
        return targetExposureWeight
                .divide(TARGET_EXPOSURE_FULL_SCORE, MATH_CONTEXT)
                .multiply(ONE_HUNDRED, MATH_CONTEXT)
                .min(ONE_HUNDRED)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal premiumDiscountScore(BigDecimal premiumDiscountPercent) {
        if (premiumDiscountPercent.compareTo(new BigDecimal("-1.00")) <= 0) {
            return new BigDecimal("100.00");
        }
        if (premiumDiscountPercent.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("85.00");
        }
        if (premiumDiscountPercent.compareTo(new BigDecimal("2.00")) <= 0) {
            return new BigDecimal("65.00");
        }
        if (premiumDiscountPercent.compareTo(new BigDecimal("5.00")) <= 0) {
            return new BigDecimal("35.00");
        }
        return new BigDecimal("10.00");
    }

    private RotationSignal signal(BigDecimal score, BigDecimal premiumDiscountPercent, boolean directSkHynixAvailable) {
        if (directSkHynixAvailable && premiumDiscountPercent.compareTo(new BigDecimal("5.00")) > 0) {
            return RotationSignal.ROTATE_TO_SK_HYNIX;
        }
        if (score.compareTo(new BigDecimal("75.00")) >= 0 && premiumDiscountPercent.compareTo(new BigDecimal("2.00")) <= 0) {
            return RotationSignal.HOLD_DRAM;
        }
        if (premiumDiscountPercent.compareTo(new BigDecimal("3.00")) > 0 || score.compareTo(new BigDecimal("45.00")) < 0) {
            return RotationSignal.AVOID_ADDING;
        }
        return RotationSignal.WAIT;
    }

    private String recommendation(RotationSignal signal, boolean directSkHynixAvailable) {
        return switch (signal) {
            case HOLD_DRAM -> "DRAM remains an efficient bridge based on current exposure and valuation inputs.";
            case ROTATE_TO_SK_HYNIX -> directSkHynixAvailable
                    ? "Direct SK hynix access is available and DRAM's premium is high, so rotation is favored."
                    : "Rotation is not actionable until direct SK hynix access is available.";
            case WAIT -> "Current inputs are mixed; wait for a better premium/discount or stronger bridge confidence.";
            case AVOID_ADDING -> "Avoid adding at current inputs because bridge efficiency is weak or DRAM is too expensive versus synthetic NAV.";
        };
    }

    private BigDecimal weighted(BigDecimal value, BigDecimal weight) {
        return value.multiply(weight, MATH_CONTEXT);
    }

    private BigDecimal clampScore(BigDecimal value) {
        return value.max(BigDecimal.ZERO).min(ONE_HUNDRED).setScale(2, RoundingMode.HALF_UP);
    }
}
