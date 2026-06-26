package com.temadison.drambuilder.dto;

import java.math.BigDecimal;

public record HoldingAttributionResponse(
        String ticker,
        String name,
        BigDecimal currentWeight,
        BigDecimal priorWeight,
        BigDecimal currentContributionPercent,
        BigDecimal priorContributionPercent,
        BigDecimal contributionChangePercent
) {
}
