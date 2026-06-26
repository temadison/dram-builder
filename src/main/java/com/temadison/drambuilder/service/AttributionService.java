package com.temadison.drambuilder.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Compares a current ETF snapshot with a prior snapshot and ranks holding-level
 * contribution changes. This service is intentionally persistence-free so the
 * attribution math can be tested independently.
 */
@Service
public class AttributionService implements AttributionCalculator {

    private static final MathContext MATH_CONTEXT = new MathContext(18, RoundingMode.HALF_UP);
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    /**
     * Calculates snapshot-to-snapshot attribution using current and prior weighted
     * contribution percentages keyed by security ticker.
     *
     * @param current current snapshot calculation inputs
     * @param prior prior snapshot calculation inputs, or null when unavailable
     * @return attribution result with top contributors sorted by absolute contribution change
     */
    @Override
    public AttributionResult calculate(AttributionSnapshotInput current, AttributionSnapshotInput prior) {
        if (prior == null) {
            return new AttributionResult(
                    false,
                    current.snapshotId(),
                    null,
                    null,
                    null,
                    List.of()
            );
        }

        Map<String, HoldingCalculation> priorByTicker = prior.holdings().stream()
                .collect(Collectors.toMap(HoldingCalculation::ticker, Function.identity(), (left, right) -> left));

        List<HoldingAttribution> topContributors = current.holdings().stream()
                .map(currentHolding -> toHoldingAttribution(currentHolding, priorByTicker.get(currentHolding.ticker())))
                .sorted(Comparator.comparing((HoldingAttribution attribution) -> attribution.contributionChangePercent().abs()).reversed())
                .toList();

        return new AttributionResult(
                true,
                current.snapshotId(),
                prior.snapshotId(),
                percentChange(current.syntheticNav(), prior.syntheticNav()),
                percentChange(current.marketPrice(), prior.marketPrice()),
                topContributors
        );
    }

    private HoldingAttribution toHoldingAttribution(HoldingCalculation current, HoldingCalculation prior) {
        BigDecimal priorWeight = prior == null ? BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP) : prior.weight();
        BigDecimal priorContribution = prior == null
                ? BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP)
                : prior.weightedContributionPercent();

        return new HoldingAttribution(
                current.ticker(),
                current.name(),
                current.weight(),
                priorWeight,
                current.weightedContributionPercent(),
                priorContribution,
                current.weightedContributionPercent().subtract(priorContribution, MATH_CONTEXT).setScale(6, RoundingMode.HALF_UP)
        );
    }

    private BigDecimal percentChange(BigDecimal current, BigDecimal prior) {
        if (prior.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Prior value cannot be zero for attribution percent change");
        }
        return current.divide(prior, MATH_CONTEXT)
                .subtract(BigDecimal.ONE, MATH_CONTEXT)
                .multiply(ONE_HUNDRED, MATH_CONTEXT)
                .setScale(6, RoundingMode.HALF_UP);
    }
}
