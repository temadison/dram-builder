package com.temadison.drambuilder.dto;

import java.math.BigDecimal;

public record HoldingResult(
        String ticker,
        String name,
        String exchange,
        String currency,
        BigDecimal weight,
        BigDecimal currentPrice,
        BigDecimal priorPrice,
        BigDecimal currentFxToUsd,
        BigDecimal priorFxToUsd,
        BigDecimal localReturnPercent,
        BigDecimal fxReturnPercent,
        BigDecimal totalReturnPercent,
        BigDecimal weightedContributionPercent
) {
}
