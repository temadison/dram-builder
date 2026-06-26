package com.temadison.drambuilder.service;

import com.temadison.drambuilder.domain.EtfHolding;
import com.temadison.drambuilder.domain.NavSnapshot;
import com.temadison.drambuilder.dto.BridgeScoreComponentResponse;
import com.temadison.drambuilder.dto.BridgeScoreRequest;
import com.temadison.drambuilder.dto.BridgeScoreResponse;
import com.temadison.drambuilder.repository.NavSnapshotRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds Bridge Score v1 inputs from the latest DRAM snapshot and returns an
 * auditable score with component values and a rotation signal.
 */
@Service
public class DramBridgeScoreService {

    private static final String DRAM_TICKER = "DRAM";
    private static final Set<String> DEFAULT_TARGET_TICKERS = Set.of("000660", "MU");
    private static final BigDecimal DEFAULT_LIQUIDITY_SCORE = new BigDecimal("70");
    private static final BigDecimal DEFAULT_TRACKING_CONFIDENCE_SCORE = new BigDecimal("65");
    private static final BigDecimal DEFAULT_TIMING_RISK_SCORE = new BigDecimal("50");

    private final NavSnapshotRepository navSnapshotRepository;
    private final BridgeScoreCalculator bridgeScoreCalculator;

    public DramBridgeScoreService(NavSnapshotRepository navSnapshotRepository, BridgeScoreCalculator bridgeScoreCalculator) {
        this.navSnapshotRepository = navSnapshotRepository;
        this.bridgeScoreCalculator = bridgeScoreCalculator;
    }

    @Transactional(readOnly = true)
    public BridgeScoreResponse latestBridgeScore(BridgeScoreRequest request) {
        BridgeScoreRequest resolvedRequest = request == null ? new BridgeScoreRequest(null, null, null, null, false) : request;
        NavSnapshot baseline = navSnapshotRepository.findFirstByHoldingSnapshotEtfTickerOrderByCreatedAtDesc(DRAM_TICKER)
                .orElseThrow(() -> new IllegalStateException("No DRAM snapshot has been created yet"));

        Set<String> targetTickers = targetTickers(resolvedRequest);
        BigDecimal targetExposureWeight = baseline.getHoldingSnapshot().getHoldings().stream()
                .filter(holding -> targetTickers.contains(normalize(holding.getSecurity().getTicker())))
                .map(EtfHolding::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BridgeScoreResult score = bridgeScoreCalculator.calculate(new BridgeScoreInput(
                targetExposureWeight,
                baseline.getPremiumDiscountPercent(),
                defaulted(resolvedRequest.liquidityScore(), DEFAULT_LIQUIDITY_SCORE),
                defaulted(resolvedRequest.trackingConfidenceScore(), DEFAULT_TRACKING_CONFIDENCE_SCORE),
                defaulted(resolvedRequest.timingRiskScore(), DEFAULT_TIMING_RISK_SCORE),
                resolvedRequest.directSkHynixAvailable()
        ));

        return new BridgeScoreResponse(
                baseline.getHoldingSnapshot().getId(),
                score.score(),
                score.rotationSignal().getDisplayName(),
                score.recommendation(),
                targetExposureWeight,
                baseline.getPremiumDiscountPercent(),
                targetTickers,
                resolvedRequest.directSkHynixAvailable(),
                new BridgeScoreComponentResponse(
                        score.targetExposureScore(),
                        score.premiumDiscountScore(),
                        score.liquidityScore(),
                        score.trackingConfidenceScore(),
                        score.timingRiskScore()
                ),
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public BridgeScoreResponse latestBridgeScore() {
        return latestBridgeScore(new BridgeScoreRequest(null, null, null, null, false));
    }

    private Set<String> targetTickers(BridgeScoreRequest request) {
        if (request.targetTickers() == null || request.targetTickers().isEmpty()) {
            return DEFAULT_TARGET_TICKERS;
        }
        return request.targetTickers().stream()
                .filter(ticker -> ticker != null && !ticker.isBlank())
                .map(this::normalize)
                .collect(Collectors.toUnmodifiableSet());
    }

    private String normalize(String ticker) {
        return ticker.toUpperCase(Locale.ROOT);
    }

    private BigDecimal defaulted(BigDecimal value, BigDecimal defaultValue) {
        return value == null ? defaultValue : value;
    }
}
