package com.temadison.drambuilder.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Applies hypothetical security and FX moves to ETF holding weights to estimate
 * the resulting DRAM move and dollar impact versus a purchase price.
 */
@Service
public class ScenarioService implements ScenarioCalculator {

    private static final MathContext MATH_CONTEXT = new MathContext(18, RoundingMode.HALF_UP);
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    /**
     * Calculates a scenario from holding weights and user-provided percent moves.
     *
     * <p>Formula:
     * total move = (1 + security move) * (1 + FX move) - 1
     * weighted contribution = holding weight * total move
     * estimated DRAM move = sum(weighted contribution)
     * projected DRAM price = baseline market price * (1 + estimated DRAM move)
     *
     * @param input baseline prices, hypothetical moves, and holding weights
     * @return scenario result sorted by absolute weighted contribution
     */
    @Override
    public ScenarioCalculationResult calculate(ScenarioInput input) {
        Map<String, BigDecimal> securityMoves = input.securityMovesPercent() == null ? Map.of() : input.securityMovesPercent();
        Map<String, BigDecimal> fxMoves = input.fxMovesPercent() == null ? Map.of() : input.fxMovesPercent();

        var holdingImpacts = input.holdings().stream()
                .map(holding -> calculateHoldingImpact(holding, securityMoves, fxMoves))
                .sorted(Comparator.comparing((ScenarioHoldingImpact impact) -> impact.weightedContributionPercent().abs()).reversed())
                .toList();

        BigDecimal estimatedMove = holdingImpacts.stream()
                .map(impact -> impact.weightedContributionPercent().divide(ONE_HUNDRED, MATH_CONTEXT))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal projectedMarketPrice = input.baselineMarketPrice()
                .multiply(BigDecimal.ONE.add(estimatedMove, MATH_CONTEXT), MATH_CONTEXT)
                .setScale(6, RoundingMode.HALF_UP);
        BigDecimal dollarImpactVsPurchasePrice = projectedMarketPrice
                .subtract(input.purchasePrice(), MATH_CONTEXT)
                .setScale(6, RoundingMode.HALF_UP);

        return new ScenarioCalculationResult(
                toPercent(estimatedMove),
                projectedMarketPrice,
                dollarImpactVsPurchasePrice,
                holdingImpacts
        );
    }

    private ScenarioHoldingImpact calculateHoldingImpact(
            ScenarioHoldingInput holding,
            Map<String, BigDecimal> securityMoves,
            Map<String, BigDecimal> fxMoves
    ) {
        BigDecimal securityMovePercent = lookupPercent(securityMoves, holding.ticker());
        BigDecimal fxMovePercent = lookupPercent(fxMoves, holding.currency());
        BigDecimal securityMove = fromPercent(securityMovePercent);
        BigDecimal fxMove = fromPercent(fxMovePercent);
        BigDecimal totalMove = BigDecimal.ONE.add(securityMove, MATH_CONTEXT)
                .multiply(BigDecimal.ONE.add(fxMove, MATH_CONTEXT), MATH_CONTEXT)
                .subtract(BigDecimal.ONE, MATH_CONTEXT);
        BigDecimal weightedContribution = holding.weight().multiply(totalMove, MATH_CONTEXT);

        return new ScenarioHoldingImpact(
                holding.ticker(),
                holding.name(),
                holding.currency(),
                holding.weight().setScale(6, RoundingMode.HALF_UP),
                securityMovePercent.setScale(6, RoundingMode.HALF_UP),
                fxMovePercent.setScale(6, RoundingMode.HALF_UP),
                toPercent(totalMove),
                toPercent(weightedContribution)
        );
    }

    private BigDecimal lookupPercent(Map<String, BigDecimal> moves, String key) {
        BigDecimal value = moves.get(key);
        if (value == null) {
            value = moves.get(key.toUpperCase(Locale.ROOT));
        }
        if (value == null) {
            value = moves.get(key.toLowerCase(Locale.ROOT));
        }
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal fromPercent(BigDecimal percent) {
        return percent.divide(ONE_HUNDRED, MATH_CONTEXT);
    }

    private BigDecimal toPercent(BigDecimal value) {
        return value.multiply(ONE_HUNDRED, MATH_CONTEXT).setScale(6, RoundingMode.HALF_UP);
    }
}
