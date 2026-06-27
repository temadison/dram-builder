package com.temadison.drambuilder.service;

import com.temadison.drambuilder.dto.MarketDataSnapshotRequest;
import com.temadison.drambuilder.dto.SnapshotRequest;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SnapshotInputValidator {

    private static final BigDecimal MAX_TOTAL_WEIGHT = BigDecimal.ONE;

    public void validate(SnapshotRequest request) {
        validateHoldingSet(
                request.holdings().stream()
                        .map(holding -> new HoldingIdentity(
                                holding.ticker(),
                                holding.exchange(),
                                holding.currency(),
                                holding.weight()
                        ))
                        .toList()
        );
    }

    public void validate(MarketDataSnapshotRequest request) {
        validateHoldingSet(
                request.holdings().stream()
                        .map(holding -> new HoldingIdentity(
                                holding.ticker(),
                                holding.exchange(),
                                holding.currency(),
                                holding.weight()
                        ))
                        .toList()
        );
    }

    private void validateHoldingSet(List<HoldingIdentity> holdings) {
        BigDecimal totalWeight = BigDecimal.ZERO;
        Set<String> seenHoldings = new HashSet<>();

        for (HoldingIdentity holding : holdings) {
            String key = normalize(holding.ticker()) + ":" + normalize(holding.exchange());
            if (!seenHoldings.add(key)) {
                throw new IllegalArgumentException("Duplicate holding in snapshot: " + key);
            }

            String currency = normalize(holding.currency());
            if (!currency.matches("[A-Z]{3}")) {
                throw new IllegalArgumentException("Currency must be a three-letter code for " + key);
            }

            totalWeight = totalWeight.add(holding.weight());
        }

        if (totalWeight.compareTo(MAX_TOTAL_WEIGHT) > 0) {
            throw new IllegalArgumentException("Total holding weight must not exceed 1.0");
        }
    }

    private String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private record HoldingIdentity(
            String ticker,
            String exchange,
            String currency,
            BigDecimal weight
    ) {
    }
}
