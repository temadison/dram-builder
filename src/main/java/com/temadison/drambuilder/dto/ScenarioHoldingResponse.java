package com.temadison.drambuilder.dto;

import java.math.BigDecimal;

public record ScenarioHoldingResponse(
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
