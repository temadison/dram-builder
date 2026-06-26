package com.temadison.drambuilder.service;

import java.math.BigDecimal;

public record HoldingCalculation(
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
