package com.temadison.drambuilder.service;

import java.math.BigDecimal;
import java.util.List;

public record AttributionSnapshotInput(
        Long snapshotId,
        BigDecimal marketPrice,
        BigDecimal syntheticNav,
        List<HoldingCalculation> holdings
) {
}
