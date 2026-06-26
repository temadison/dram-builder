package com.temadison.drambuilder.service;

import com.temadison.drambuilder.dto.HoldingInput;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Calculates a normalized synthetic NAV for an ETF from manually entered holding weights,
 * prices, and FX rates. The Release 0.2 model treats current ETF market price as the anchor
 * and estimates fair value by applying each holding's weighted USD return factor.
 */
@Service
public class SyntheticNavService implements SyntheticNavCalculator {

    private static final MathContext MATH_CONTEXT = new MathContext(18, RoundingMode.HALF_UP);
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    /**
     * Calculates synthetic NAV and premium/discount.
     *
     * <p>Formula:
     * total return = ((current price * current FX) / (prior price * prior FX)) - 1
     * weighted contribution = holding weight * total return
     * synthetic NAV = market price * (1 + sum(weighted contribution))
     * premium/discount = (market price / synthetic NAV - 1) * 100
     *
     * @param marketPrice current ETF market price
     * @param holdings manually entered holdings
     * @return normalized synthetic NAV calculation result
     */
    @Override
    public SyntheticNavResult calculate(BigDecimal marketPrice, List<HoldingInput> holdings) {
        validateWeights(holdings);

        List<HoldingCalculation> holdingCalculations = holdings.stream()
                .map(this::calculateHolding)
                .sorted(Comparator.comparing(HoldingCalculation::weightedContributionPercent).reversed())
                .toList();

        BigDecimal weightedMove = holdingCalculations.stream()
                .map(result -> result.weightedContributionPercent().divide(ONE_HUNDRED, MATH_CONTEXT))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal syntheticNav = marketPrice
                .multiply(BigDecimal.ONE.add(weightedMove, MATH_CONTEXT), MATH_CONTEXT)
                .setScale(6, RoundingMode.HALF_UP);

        BigDecimal premiumDiscountPercent = marketPrice
                .divide(syntheticNav, MATH_CONTEXT)
                .subtract(BigDecimal.ONE, MATH_CONTEXT)
                .multiply(ONE_HUNDRED, MATH_CONTEXT)
                .setScale(6, RoundingMode.HALF_UP);

        return new SyntheticNavResult(
                syntheticNav,
                weightedMove.multiply(ONE_HUNDRED, MATH_CONTEXT).setScale(6, RoundingMode.HALF_UP),
                premiumDiscountPercent,
                holdingCalculations
        );
    }

    private HoldingCalculation calculateHolding(HoldingInput holding) {
        BigDecimal localReturn = holding.currentPrice()
                .divide(holding.priorPrice(), MATH_CONTEXT)
                .subtract(BigDecimal.ONE, MATH_CONTEXT);

        BigDecimal fxReturn = holding.currentFxToUsd()
                .divide(holding.priorFxToUsd(), MATH_CONTEXT)
                .subtract(BigDecimal.ONE, MATH_CONTEXT);

        BigDecimal currentUsd = holding.currentPrice().multiply(holding.currentFxToUsd(), MATH_CONTEXT);
        BigDecimal priorUsd = holding.priorPrice().multiply(holding.priorFxToUsd(), MATH_CONTEXT);
        BigDecimal totalReturn = currentUsd.divide(priorUsd, MATH_CONTEXT).subtract(BigDecimal.ONE, MATH_CONTEXT);
        BigDecimal weightedContribution = holding.weight().multiply(totalReturn, MATH_CONTEXT);

        return new HoldingCalculation(
                holding.ticker(),
                holding.name(),
                holding.exchange(),
                holding.currency(),
                scale(holding.weight()),
                holding.currentPrice(),
                holding.priorPrice(),
                holding.currentFxToUsd(),
                holding.priorFxToUsd(),
                toPercent(localReturn),
                toPercent(fxReturn),
                toPercent(totalReturn),
                toPercent(weightedContribution)
        );
    }

    private void validateWeights(List<HoldingInput> holdings) {
        BigDecimal totalWeight = holdings.stream()
                .map(HoldingInput::weight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalWeight.compareTo(BigDecimal.ZERO) <= 0 || totalWeight.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Holding weights must total more than 0 and no more than 1.0");
        }
    }

    private BigDecimal toPercent(BigDecimal value) {
        return value.multiply(ONE_HUNDRED, MATH_CONTEXT).setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(6, RoundingMode.HALF_UP);
    }
}
