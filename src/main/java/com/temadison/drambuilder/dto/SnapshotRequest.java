package com.temadison.drambuilder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SnapshotRequest(
        LocalDate asOfDate,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal marketPrice,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal purchasePrice,
        @NotEmpty List<@Valid HoldingInput> holdings
) {
}
