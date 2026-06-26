package com.temadison.drambuilder.service;

import com.temadison.drambuilder.domain.Etf;
import com.temadison.drambuilder.domain.EtfHolding;
import com.temadison.drambuilder.domain.EtfHoldingSnapshot;
import com.temadison.drambuilder.domain.NavSnapshot;
import com.temadison.drambuilder.domain.Security;
import com.temadison.drambuilder.dto.AttributionResponse;
import com.temadison.drambuilder.dto.HoldingAttributionResponse;
import com.temadison.drambuilder.dto.HoldingInput;
import com.temadison.drambuilder.dto.HoldingResult;
import com.temadison.drambuilder.dto.SnapshotRequest;
import com.temadison.drambuilder.dto.SnapshotResponse;
import com.temadison.drambuilder.repository.EtfHoldingSnapshotRepository;
import com.temadison.drambuilder.repository.EtfRepository;
import com.temadison.drambuilder.repository.NavSnapshotRepository;
import com.temadison.drambuilder.repository.SecurityRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates manual DRAM snapshot ingestion, persistence, synthetic NAV calculation,
 * attribution, and response mapping for the DRAM snapshot API.
 */
@Service
public class DramSnapshotService {

    private static final String DRAM_TICKER = "DRAM";
    private static final String DRAM_NAME = "Roundhill Memory ETF";

    private final EtfRepository etfRepository;
    private final SecurityRepository securityRepository;
    private final EtfHoldingSnapshotRepository holdingSnapshotRepository;
    private final NavSnapshotRepository navSnapshotRepository;
    private final SyntheticNavCalculator syntheticNavCalculator;
    private final AttributionCalculator attributionCalculator;

    public DramSnapshotService(
            EtfRepository etfRepository,
            SecurityRepository securityRepository,
            EtfHoldingSnapshotRepository holdingSnapshotRepository,
            NavSnapshotRepository navSnapshotRepository,
            SyntheticNavCalculator syntheticNavCalculator,
            AttributionCalculator attributionCalculator
    ) {
        this.etfRepository = etfRepository;
        this.securityRepository = securityRepository;
        this.holdingSnapshotRepository = holdingSnapshotRepository;
        this.navSnapshotRepository = navSnapshotRepository;
        this.syntheticNavCalculator = syntheticNavCalculator;
        this.attributionCalculator = attributionCalculator;
    }

    @Transactional
    public SnapshotResponse createSnapshot(SnapshotRequest request) {
        Etf etf = etfRepository.findByTicker(DRAM_TICKER)
                .orElseGet(() -> etfRepository.save(new Etf(DRAM_TICKER, DRAM_NAME)));
        NavSnapshot priorSnapshot = navSnapshotRepository.findFirstByHoldingSnapshotEtfTickerOrderByCreatedAtDesc(DRAM_TICKER)
                .orElse(null);
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
        SyntheticNavResult navResult = syntheticNavCalculator.calculate(request.marketPrice(), request.holdings());
        NavSnapshot navSnapshot = navSnapshotRepository.save(new NavSnapshot(
                savedHoldingSnapshot,
                navResult.syntheticNav(),
                request.marketPrice(),
                navResult.premiumDiscountPercent(),
                navResult.estimatedEtfMovePercent(),
                now
        ));

        return toResponse(navSnapshot, navResult, priorSnapshot);
    }

    @Transactional
    public SnapshotResponse latestSnapshot() {
        List<NavSnapshot> latestSnapshots = navSnapshotRepository.findTop2ByHoldingSnapshotEtfTickerOrderByCreatedAtDesc(DRAM_TICKER);
        if (latestSnapshots.isEmpty()) {
            throw new IllegalStateException("No DRAM snapshot has been created yet");
        }

        NavSnapshot navSnapshot = latestSnapshots.getFirst();
        NavSnapshot priorSnapshot = latestSnapshots.size() > 1 ? latestSnapshots.get(1) : null;
        SyntheticNavResult navResult = syntheticNavCalculator.calculate(
                navSnapshot.getMarketPrice(),
                toHoldingInputs(navSnapshot)
        );
        return toResponse(navSnapshot, navResult, priorSnapshot);
    }

    private SnapshotResponse toResponse(NavSnapshot navSnapshot, SyntheticNavResult navResult, NavSnapshot priorSnapshot) {
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
                toHoldingResults(navResult.holdings()),
                toAttributionResponse(navSnapshot, navResult, priorSnapshot)
        );
    }

    private List<HoldingInput> toHoldingInputs(NavSnapshot navSnapshot) {
        return navSnapshot.getHoldingSnapshot().getHoldings().stream()
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
                .toList();
    }

    private AttributionResponse toAttributionResponse(NavSnapshot navSnapshot, SyntheticNavResult navResult, NavSnapshot priorSnapshot) {
        AttributionSnapshotInput currentInput = new AttributionSnapshotInput(
                navSnapshot.getHoldingSnapshot().getId(),
                navSnapshot.getMarketPrice(),
                navSnapshot.getSyntheticNav(),
                navResult.holdings()
        );

        AttributionSnapshotInput priorInput = null;
        if (priorSnapshot != null) {
            SyntheticNavResult priorNavResult = syntheticNavCalculator.calculate(
                    priorSnapshot.getMarketPrice(),
                    toHoldingInputs(priorSnapshot)
            );
            priorInput = new AttributionSnapshotInput(
                    priorSnapshot.getHoldingSnapshot().getId(),
                    priorSnapshot.getMarketPrice(),
                    priorSnapshot.getSyntheticNav(),
                    priorNavResult.holdings()
            );
        }

        AttributionResult attribution = attributionCalculator.calculate(currentInput, priorInput);
        return new AttributionResponse(
                attribution.hasPriorSnapshot(),
                attribution.currentSnapshotId(),
                attribution.priorSnapshotId(),
                attribution.syntheticNavChangePercent(),
                attribution.marketPriceChangePercent(),
                attribution.topContributors().stream()
                        .map(holding -> new HoldingAttributionResponse(
                                holding.ticker(),
                                holding.name(),
                                holding.currentWeight(),
                                holding.priorWeight(),
                                holding.currentContributionPercent(),
                                holding.priorContributionPercent(),
                                holding.contributionChangePercent()
                        ))
                        .toList()
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
