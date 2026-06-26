package com.temadison.drambuilder.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ScenarioInput(
        BigDecimal baselineMarketPrice,
        BigDecimal purchasePrice,
        Map<String, BigDecimal> securityMovesPercent,
        Map<String, BigDecimal> fxMovesPercent,
        List<ScenarioHoldingInput> holdings
) {
}
