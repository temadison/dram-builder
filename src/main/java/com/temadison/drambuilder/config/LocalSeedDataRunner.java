package com.temadison.drambuilder.config;

import com.temadison.drambuilder.dto.HoldingInput;
import com.temadison.drambuilder.dto.SnapshotRequest;
import com.temadison.drambuilder.service.DramSnapshotService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class LocalSeedDataRunner implements CommandLineRunner {

    private final DramSnapshotService dramSnapshotService;

    public LocalSeedDataRunner(DramSnapshotService dramSnapshotService) {
        this.dramSnapshotService = dramSnapshotService;
    }

    @Override
    public void run(String... args) {
        if (snapshotExists()) {
            return;
        }

        dramSnapshotService.createSnapshot(baselineSnapshot());
        dramSnapshotService.createSnapshot(followUpSnapshot());
    }

    private boolean snapshotExists() {
        try {
            dramSnapshotService.latestSnapshot();
            return true;
        } catch (IllegalStateException exception) {
            return false;
        }
    }

    private SnapshotRequest baselineSnapshot() {
        return new SnapshotRequest(
                LocalDate.of(2026, 6, 25),
                new BigDecimal("80.00"),
                new BigDecimal("76.31"),
                List.of(
                        holding("000660", "SK hynix", "KRX", "KRW", "0.25", "110000", "100000", "0.00080", "0.00080"),
                        holding("MU", "Micron Technology", "NASDAQ", "USD", "0.20", "105", "100", "1", "1"),
                        holding("005930", "Samsung Electronics", "KRX", "KRW", "0.15", "77600", "80000", "0.00082", "0.00080"),
                        holding("ASML", "ASML Holding", "NASDAQ", "USD", "0.05", "1020", "1000", "1", "1")
                )
        );
    }

    private SnapshotRequest followUpSnapshot() {
        return new SnapshotRequest(
                LocalDate.of(2026, 6, 26),
                new BigDecimal("81.50"),
                new BigDecimal("76.31"),
                List.of(
                        holding("000660", "SK hynix", "KRX", "KRW", "0.26", "114000", "110000", "0.00081", "0.00080"),
                        holding("MU", "Micron Technology", "NASDAQ", "USD", "0.19", "108", "105", "1", "1"),
                        holding("005930", "Samsung Electronics", "KRX", "KRW", "0.15", "79000", "77600", "0.00081", "0.00082"),
                        holding("ASML", "ASML Holding", "NASDAQ", "USD", "0.05", "1015", "1020", "1", "1")
                )
        );
    }

    private HoldingInput holding(
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
