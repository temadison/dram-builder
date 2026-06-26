package com.temadison.drambuilder.service;

import java.math.BigDecimal;
import java.util.List;

public record ScenarioCalculationResult(
        BigDecimal estimatedMovePercent,
        BigDecimal projectedMarketPrice,
        BigDecimal dollarImpactVsPurchasePrice,
        List<ScenarioHoldingImpact> holdings
) {
}
