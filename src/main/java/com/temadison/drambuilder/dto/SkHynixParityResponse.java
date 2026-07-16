package com.temadison.drambuilder.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SkHynixParityResponse(
        LocalDate date,
        BigDecimal adrPrice,
        BigDecimal krxPrice,
        BigDecimal krwUsdRate,
        BigDecimal localEquivalentUsdPerAdr,
        BigDecimal premiumDiscountPercent,
        Instant adrObservedAt,
        Instant krxObservedAt,
        Instant fxObservedAt
) {
}
