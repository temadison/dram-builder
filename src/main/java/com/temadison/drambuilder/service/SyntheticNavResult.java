package com.temadison.drambuilder.service;

import java.math.BigDecimal;
import java.util.List;

public record SyntheticNavResult(
        BigDecimal syntheticNav,
        BigDecimal estimatedEtfMovePercent,
        BigDecimal premiumDiscountPercent,
        List<HoldingCalculation> holdings
) {
}
