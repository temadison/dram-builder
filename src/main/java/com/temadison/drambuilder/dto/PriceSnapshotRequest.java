package com.temadison.drambuilder.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record PriceSnapshotRequest(
        @NotBlank String ticker,
        @NotBlank String name,
        @NotBlank String exchange,
        @NotBlank String currency,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        @NotBlank String source,
        Instant observedAt
) {
}
