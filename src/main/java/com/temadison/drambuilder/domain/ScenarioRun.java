package com.temadison.drambuilder.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scenario_run")
public class ScenarioRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "holding_snapshot_id", nullable = false)
    private EtfHoldingSnapshot holdingSnapshot;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal purchasePrice;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal estimatedMovePercent;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal projectedMarketPrice;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal dollarImpactVsPurchasePrice;

    @Column(nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "scenarioRun", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "display_order")
    private List<ScenarioResult> results = new ArrayList<>();

    protected ScenarioRun() {
    }

    public ScenarioRun(
            EtfHoldingSnapshot holdingSnapshot,
            String name,
            BigDecimal purchasePrice,
            BigDecimal estimatedMovePercent,
            BigDecimal projectedMarketPrice,
            BigDecimal dollarImpactVsPurchasePrice,
            Instant createdAt
    ) {
        this.holdingSnapshot = holdingSnapshot;
        this.name = name;
        this.purchasePrice = purchasePrice;
        this.estimatedMovePercent = estimatedMovePercent;
        this.projectedMarketPrice = projectedMarketPrice;
        this.dollarImpactVsPurchasePrice = dollarImpactVsPurchasePrice;
        this.createdAt = createdAt;
    }

    public void addResult(ScenarioResult result) {
        results.add(result);
        result.attachTo(this);
    }

    public Long getId() {
        return id;
    }

    public EtfHoldingSnapshot getHoldingSnapshot() {
        return holdingSnapshot;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public BigDecimal getEstimatedMovePercent() {
        return estimatedMovePercent;
    }

    public BigDecimal getProjectedMarketPrice() {
        return projectedMarketPrice;
    }

    public BigDecimal getDollarImpactVsPurchasePrice() {
        return dollarImpactVsPurchasePrice;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<ScenarioResult> getResults() {
        return results;
    }
}
