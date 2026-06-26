package com.temadison.drambuilder.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record HoldingInput(
        @NotBlank String ticker,
        @NotBlank String name,
        @NotBlank String exchange,
        @NotBlank String currency,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal weight,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal currentPrice,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal priorPrice,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal currentFxToUsd,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal priorFxToUsd
) {
}
