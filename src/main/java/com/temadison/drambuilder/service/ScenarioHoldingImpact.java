package com.temadison.drambuilder.service;

import java.math.BigDecimal;

public record ScenarioHoldingImpact(
        String ticker,
        String name,
        String currency,
        BigDecimal weight,
        BigDecimal securityMovePercent,
        BigDecimal fxMovePercent,
        BigDecimal totalMovePercent,
        BigDecimal weightedContributionPercent
) {
}
