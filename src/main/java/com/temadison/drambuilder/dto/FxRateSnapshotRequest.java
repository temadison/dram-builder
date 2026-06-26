package com.temadison.drambuilder.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record FxRateSnapshotRequest(
        @NotBlank String baseCurrency,
        @NotBlank String quoteCurrency,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal rate,
        @NotBlank String source,
        Instant observedAt
) {
}
