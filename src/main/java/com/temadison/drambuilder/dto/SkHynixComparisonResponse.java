package com.temadison.drambuilder.dto;

import java.util.List;

public record SkHynixComparisonResponse(
        String adrTicker,
        String localTicker,
        String localExchange,
        int adrPerLocalShare,
        SkHynixParityResponse latestParity,
        List<SkHynixComparisonPointResponse> performance
) {
}
