package com.temadison.drambuilder.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SkHynixComparisonPointResponse(
        LocalDate date,
        String symbol,
        String label,
        BigDecimal price,
        BigDecimal sharesOutstanding,
        BigDecimal marketCapUsd
) {
}
