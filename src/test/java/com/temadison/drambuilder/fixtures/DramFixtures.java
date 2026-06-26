package com.temadison.drambuilder.fixtures;

import com.temadison.drambuilder.dto.BridgeScoreRequest;
import com.temadison.drambuilder.dto.HoldingInput;
import com.temadison.drambuilder.dto.ScenarioRequest;
import com.temadison.drambuilder.dto.SnapshotRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DramFixtures {

    public static final BigDecimal PURCHASE_PRICE = new BigDecimal("76.31");

    private DramFixtures() {
    }

    public static SnapshotRequest baselineSnapshot() {
        return new SnapshotRequest(
                LocalDate.of(2026, 6, 25),
                new BigDecimal("80.00"),
                PURCHASE_PRICE,
                List.of(
                        holding("000660", "SK hynix", "KRX", "KRW", "0.25", "110000", "100000", "0.00080", "0.00080"),
                        holding("MU", "Micron Technology", "NASDAQ", "USD", "0.20", "105", "100", "1", "1"),
                        holding("005930", "Samsung Electronics", "KRX", "KRW", "0.15", "77600", "80000", "0.00082", "0.00080"),
                        holding("ASML", "ASML Holding", "NASDAQ", "USD", "0.05", "1020", "1000", "1", "1")
                )
        );
    }

    public static SnapshotRequest followUpSnapshot() {
        return new SnapshotRequest(
                LocalDate.of(2026, 6, 26),
                new BigDecimal("81.50"),
                PURCHASE_PRICE,
                List.of(
                        holding("000660", "SK hynix", "KRX", "KRW", "0.26", "114000", "110000", "0.00081", "0.00080"),
                        holding("MU", "Micron Technology", "NASDAQ", "USD", "0.19", "108", "105", "1", "1"),
                        holding("005930", "Samsung Electronics", "KRX", "KRW", "0.15", "79000", "77600", "0.00081", "0.00082"),
                        holding("ASML", "ASML Holding", "NASDAQ", "USD", "0.05", "1015", "1020", "1", "1")
                )
        );
    }

    public static ScenarioRequest upsideScenario() {
        return new ScenarioRequest(
                "HBM upside with KRW tailwind",
                Map.of(
                        "000660", new BigDecimal("10"),
                        "MU", new BigDecimal("5"),
                        "005930", new BigDecimal("-3")
                ),
                Map.of("KRW", new BigDecimal("2")),
                PURCHASE_PRICE
        );
    }

    public static BridgeScoreRequest bridgeScoreOverrides() {
        return new BridgeScoreRequest(
                Set.of("000660", "MU"),
                new BigDecimal("75"),
                new BigDecimal("70"),
                new BigDecimal("55"),
                false
        );
    }

    private static HoldingInput holding(
            String ticker,
            String name,
            String exchange,
            String currency,
            String weight,
            String currentPrice,
            String priorPrice,
            String currentFxToUsd,
            String priorFxToUsd
    ) {
        return new HoldingInput(
                ticker,
                name,
                exchange,
                currency,
                new BigDecimal(weight),
                new BigDecimal(currentPrice),
                new BigDecimal(priorPrice),
                new BigDecimal(currentFxToUsd),
                new BigDecimal(priorFxToUsd)
        );
    }
}
