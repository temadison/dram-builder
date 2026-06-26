package com.temadison.drambuilder.service;

import com.temadison.drambuilder.domain.EtfHoldingSnapshot;
import com.temadison.drambuilder.domain.NavSnapshot;
import com.temadison.drambuilder.domain.ScenarioResult;
import com.temadison.drambuilder.domain.ScenarioRun;
import com.temadison.drambuilder.dto.ScenarioHoldingResponse;
import com.temadison.drambuilder.dto.ScenarioRequest;
import com.temadison.drambuilder.dto.ScenarioResponse;
import com.temadison.drambuilder.repository.NavSnapshotRepository;
import com.temadison.drambuilder.repository.ScenarioRunRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * Coordinates scenario execution against the latest DRAM snapshot and persists
 * aggregate and holding-level scenario results.
 */
@Service
public class DramScenarioService {

    private static final String DRAM_TICKER = "DRAM";

    private final NavSnapshotRepository navSnapshotRepository;
    private final ScenarioRunRepository scenarioRunRepository;
    private final ScenarioCalculator scenarioCalculator;

    public DramScenarioService(
            NavSnapshotRepository navSnapshotRepository,
            ScenarioRunRepository scenarioRunRepository,
            ScenarioCalculator scenarioCalculator
    ) {
        this.navSnapshotRepository = navSnapshotRepository;
        this.scenarioRunRepository = scenarioRunRepository;
        this.scenarioCalculator = scenarioCalculator;
    }

    @Transactional
    public ScenarioResponse runScenario(ScenarioRequest request) {
        NavSnapshot baseline = navSnapshotRepository.findFirstByHoldingSnapshotEtfTickerOrderByCreatedAtDesc(DRAM_TICKER)
                .orElseThrow(() -> new IllegalStateException("No DRAM snapshot has been created yet"));
        EtfHoldingSnapshot holdingSnapshot = baseline.getHoldingSnapshot();
        BigDecimal purchasePrice = request.purchasePrice() == null ? holdingSnapshot.getPurchasePrice() : request.purchasePrice();

        ScenarioCalculationResult calculation = scenarioCalculator.calculate(new ScenarioInput(
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
