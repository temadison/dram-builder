package com.temadison.drambuilder.service;

import java.math.BigDecimal;

public record HoldingAttribution(
        String ticker,
        String name,
        BigDecimal currentWeight,
        BigDecimal priorWeight,
        BigDecimal currentContributionPercent,
        BigDecimal priorContributionPercent,
        BigDecimal contributionChangePercent
) {
}
