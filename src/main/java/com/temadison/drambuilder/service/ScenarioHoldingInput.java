package com.temadison.drambuilder.service;

import java.math.BigDecimal;

public record ScenarioHoldingInput(
        String ticker,
        String name,
        String currency,
        BigDecimal weight
) {
}
