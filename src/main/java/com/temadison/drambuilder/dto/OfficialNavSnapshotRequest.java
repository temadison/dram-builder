package com.temadison.drambuilder.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record OfficialNavSnapshotRequest(
        @NotBlank String ticker,
        @NotBlank String name,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal nav,
        @NotBlank String currency,
        @NotBlank String source,
        @NotNull LocalDate asOfDate,
        Instant observedAt
) {
}
