package com.temadison.drambuilder.service;

import com.temadison.drambuilder.domain.Etf;
import com.temadison.drambuilder.domain.EtfHolding;
import com.temadison.drambuilder.domain.EtfHoldingSnapshot;
import com.temadison.drambuilder.domain.NavSnapshot;
import com.temadison.drambuilder.domain.Security;
import com.temadison.drambuilder.dto.HoldingInput;
import com.temadison.drambuilder.dto.HoldingResult;
import com.temadison.drambuilder.dto.SnapshotRequest;
import com.temadison.drambuilder.dto.SnapshotResponse;
import com.temadison.drambuilder.repository.EtfHoldingSnapshotRepository;
import com.temadison.drambuilder.repository.EtfRepository;
import com.temadison.drambuilder.repository.NavSnapshotRepository;
import com.temadison.drambuilder.repository.SecurityRepository;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Coordinates manual DRAM snapshot ingestion, persistence, synthetic NAV calculation,
 * and response mapping for the Release 0.2 API.
 */
@Service
public class DramSnapshotService {

    private static final String DRAM_TICKER = "DRAM";
    private static final String DRAM_NAME = "Roundhill Memory ETF";

    private final EtfRepository etfRepository;
    private final SecurityRepository securityRepository;
    private final EtfHoldingSnapshotRepository holdingSnapshotRepository;
    private final NavSnapshotRepository navSnapshotRepository;
    private final SyntheticNavService syntheticNavService;

    public DramSnapshotService(
            EtfRepository etfRepository,
            SecurityRepository securityRepository,
            EtfHoldingSnapshotRepository holdingSnapshotRepository,
            NavSnapshotRepository navSnapshotRepository,
            SyntheticNavService syntheticNavService
    ) {
        this.etfRepository = etfRepository;
        this.securityRepository = securityRepository;
        this.holdingSnapshotRepository = holdingSnapshotRepository;
        this.navSnapshotRepository = navSnapshotRepository;
        this.syntheticNavService = syntheticNavService;
    }

    @Transactional
    public SnapshotResponse createSnapshot(SnapshotRequest request) {
        Etf etf = etfRepository.findByTicker(DRAM_TICKER)
                .orElseGet(() -> etfRepository.save(new Etf(DRAM_TICKER, DRAM_NAME)));
        Instant now = Instant.now();

        EtfHoldingSnapshot holdingSnapshot = new EtfHoldingSnapshot(
                etf,
                request.asOfDate() == null ? LocalDate.now() : request.asOfDate(),
                request.marketPrice(),
                request.purchasePrice(),
                now
        );

        for (HoldingInput holding : request.holdings()) {
            Security security = securityRepository.findByTickerAndExchange(holding.ticker(), holding.exchange())
                    .orElseGet(() -> securityRepository.save(new Security(
                            holding.ticker(),
                            holding.name(),
                            holding.exchange(),
                            holding.currency()
                    )));
            holdingSnapshot.addHolding(new EtfHolding(
                    security,
                    holding.weight(),
                    holding.currentPrice(),
                    holding.priorPrice(),
                    holding.currentFxToUsd(),
                    holding.priorFxToUsd()
            ));
        }

        EtfHoldingSnapshot savedHoldingSnapshot = holdingSnapshotRepository.save(holdingSnapshot);
        SyntheticNavResult navResult = syntheticNavService.calculate(request.marketPrice(), request.holdings());
        NavSnapshot navSnapshot = navSnapshotRepository.save(new NavSnapshot(
                savedHoldingSnapshot,
                navResult.syntheticNav(),
                request.marketPrice(),
                navResult.premiumDiscountPercent(),
                navResult.estimatedEtfMovePercent(),
                now
        ));

        return toResponse(navSnapshot, navResult);
    }

    @Transactional
    public SnapshotResponse latestSnapshot() {
        NavSnapshot navSnapshot = navSnapshotRepository.findFirstByHoldingSnapshotEtfTickerOrderByCreatedAtDesc(DRAM_TICKER)
                .orElseThrow(() -> new IllegalStateException("No DRAM snapshot has been created yet"));

        SyntheticNavResult navResult = syntheticNavService.calculate(
                navSnapshot.getMarketPrice(),
                navSnapshot.getHoldingSnapshot().getHoldings().stream()
                        .map(holding -> new HoldingInput(
                                holding.getSecurity().getTicker(),
                                holding.getSecurity().getName(),
                                holding.getSecurity().getExchange(),
                                holding.getSecurity().getCurrency(),
                                holding.getWeight(),
                                holding.getCurrentPrice(),
                                holding.getPriorPrice(),
                                holding.getCurrentFxToUsd(),
                                holding.getPriorFxToUsd()
                        ))
                        .toList()
        );
        return toResponse(navSnapshot, navResult);
    }

    private SnapshotResponse toResponse(NavSnapshot navSnapshot, SyntheticNavResult navResult) {
        EtfHoldingSnapshot holdingSnapshot = navSnapshot.getHoldingSnapshot();
        return new SnapshotResponse(
                holdingSnapshot.getId(),
                holdingSnapshot.getEtf().getTicker(),
                holdingSnapshot.getAsOfDate(),
                holdingSnapshot.getMarketPrice(),
                holdingSnapshot.getPurchasePrice(),
                navSnapshot.getSyntheticNav(),
                navSnapshot.getEstimatedEtfMovePercent(),
                navSnapshot.getPremiumDiscountPercent(),
                navSnapshot.getCreatedAt(),
                toHoldingResults(navResult.holdings())
        );
    }

    private List<HoldingResult> toHoldingResults(List<HoldingCalculation> holdings) {
        return holdings.stream()
                .map(holding -> new HoldingResult(
                        holding.ticker(),
                        holding.name(),
                        holding.exchange(),
                        holding.currency(),
                        holding.weight(),
                        holding.currentPrice(),
                        holding.priorPrice(),
                        holding.currentFxToUsd(),
                        holding.priorFxToUsd(),
                        holding.localReturnPercent(),
                        holding.fxReturnPercent(),
                        holding.totalReturnPercent(),
                        holding.weightedContributionPercent()
                ))
                .toList();
    }
}
