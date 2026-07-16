package com.temadison.drambuilder.dto;

import java.util.List;

public record SkHynixComparisonResponse(
        String adrTicker,
        String localTicker,
        String localExchange,
        int adrPerLocalShare,
        java.math.BigDecimal micronSharesOutstanding,
        java.math.BigDecimal skHynixLocalSharesOutstanding,
        SkHynixParityResponse latestParity,
        List<SkHynixComparisonPointResponse> performance
) {
}
