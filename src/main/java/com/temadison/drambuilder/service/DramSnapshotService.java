package com.temadison.drambuilder.service;

import com.temadison.drambuilder.domain.Etf;
import com.temadison.drambuilder.domain.EtfHolding;
import com.temadison.drambuilder.domain.EtfHoldingSnapshot;
import com.temadison.drambuilder.domain.NavSnapshot;
import com.temadison.drambuilder.domain.ScenarioResult;
import com.temadison.drambuilder.domain.ScenarioRun;
import com.temadison.drambuilder.domain.Security;
import com.temadison.drambuilder.dto.AttributionResponse;
import com.temadison.drambuilder.dto.HoldingAttributionResponse;
import com.temadison.drambuilder.dto.HoldingInput;
import com.temadison.drambuilder.dto.HoldingResult;
import com.temadison.drambuilder.dto.ScenarioHoldingResponse;
import com.temadison.drambuilder.dto.ScenarioRequest;
import com.temadison.drambuilder.dto.ScenarioResponse;
import com.temadison.drambuilder.dto.SnapshotRequest;
import com.temadison.drambuilder.dto.SnapshotResponse;
import com.temadison.drambuilder.repository.EtfHoldingSnapshotRepository;
import com.temadison.drambuilder.repository.EtfRepository;
import com.temadison.drambuilder.repository.NavSnapshotRepository;
import com.temadison.drambuilder.repository.ScenarioRunRepository;
import com.temadison.drambuilder.repository.SecurityRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

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
    private final ScenarioRunRepository scenarioRunRepository;
    private final SyntheticNavService syntheticNavService;
    private final AttributionService attributionService;
    private final ScenarioService scenarioService;

    public DramSnapshotService(
            EtfRepository etfRepository,
            SecurityRepository securityRepository,
            EtfHoldingSnapshotRepository holdingSnapshotRepository,
            NavSnapshotRepository navSnapshotRepository,
            ScenarioRunRepository scenarioRunRepository,
            SyntheticNavService syntheticNavService,
            AttributionService attributionService,
            ScenarioService scenarioService
    ) {
        this.etfRepository = etfRepository;
        this.securityRepository = securityRepository;
        this.holdingSnapshotRepository = holdingSnapshotRepository;
        this.navSnapshotRepository = navSnapshotRepository;
        this.scenarioRunRepository = scenarioRunRepository;
        this.syntheticNavService = syntheticNavService;
        this.attributionService = attributionService;
        this.scenarioService = scenarioService;
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
        SyntheticNavResult navResult = syntheticNavService.calculate(request.marketPrice(), request.holdings());
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
        SyntheticNavResult navResult = syntheticNavService.calculate(
                navSnapshot.getMarketPrice(),
                toHoldingInputs(navSnapshot)
        );
        return toResponse(navSnapshot, navResult, priorSnapshot);
    }

    @Transactional
    public ScenarioResponse runScenario(ScenarioRequest request) {
        NavSnapshot baseline = navSnapshotRepository.findFirstByHoldingSnapshotEtfTickerOrderByCreatedAtDesc(DRAM_TICKER)
                .orElseThrow(() -> new IllegalStateException("No DRAM snapshot has been created yet"));
        EtfHoldingSnapshot holdingSnapshot = baseline.getHoldingSnapshot();
        BigDecimal purchasePrice = request.purchasePrice() == null ? holdingSnapshot.getPurchasePrice() : request.purchasePrice();

        ScenarioCalculationResult calculation = scenarioService.calculate(new ScenarioInput(
                baseline.getMarketPrice(),
                purchasePrice,
                request.securityMovesPercent(),
                request.fxMovesPercent(),
                holdingSnapshot.getHoldings().stream()
                        .map(holding -> new ScenarioHoldingInput(
                                holding.getSecurity().getTicker(),
                                holding.getSecurity().getName(),
                                holding.getSecurity().getCurrency(),
                                holding.getWeight()
                        ))
                        .toList()
        ));

        ScenarioRun scenarioRun = new ScenarioRun(
                holdingSnapshot,
                scenarioName(request),
                purchasePrice,
                calculation.estimatedMovePercent(),
                calculation.projectedMarketPrice(),
                calculation.dollarImpactVsPurchasePrice(),
                Instant.now()
        );
        calculation.holdings().forEach(impact -> scenarioRun.addResult(new ScenarioResult(
                impact.ticker(),
                impact.name(),
                impact.currency(),
                impact.weight(),
                impact.securityMovePercent(),
                impact.fxMovePercent(),
                impact.totalMovePercent(),
                impact.weightedContributionPercent()
        )));

        ScenarioRun savedScenarioRun = scenarioRunRepository.save(scenarioRun);
        return toScenarioResponse(savedScenarioRun, baseline.getMarketPrice());
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
            SyntheticNavResult priorNavResult = syntheticNavService.calculate(
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

        AttributionResult attribution = attributionService.calculate(currentInput, priorInput);
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

    private ScenarioResponse toScenarioResponse(ScenarioRun scenarioRun, BigDecimal baselineMarketPrice) {
        return new ScenarioResponse(
                scenarioRun.getId(),
                scenarioRun.getHoldingSnapshot().getId(),
                scenarioRun.getName(),
                baselineMarketPrice,
                scenarioRun.getPurchasePrice(),
                scenarioRun.getEstimatedMovePercent(),
                scenarioRun.getProjectedMarketPrice(),
                scenarioRun.getDollarImpactVsPurchasePrice(),
                scenarioRun.getCreatedAt(),
                scenarioRun.getResults().stream()
                        .map(result -> new ScenarioHoldingResponse(
                                result.getTicker(),
                                result.getName(),
                                result.getCurrency(),
                                result.getWeight(),
                                result.getSecurityMovePercent(),
                                result.getFxMovePercent(),
                                result.getTotalMovePercent(),
                                result.getWeightedContributionPercent()
                        ))
                        .toList()
        );
    }

    private String scenarioName(ScenarioRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            return "Manual scenario";
        }
        return request.name();
    }
}
