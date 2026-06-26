package com.temadison.drambuilder.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "scenario_result")
public class ScenarioResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_run_id", nullable = false)
    private ScenarioRun scenarioRun;

    @Column(nullable = false, length = 32)
    private String ticker;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal weight;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal securityMovePercent;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal fxMovePercent;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal totalMovePercent;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal weightedContributionPercent;

    protected ScenarioResult() {
    }

    public ScenarioResult(
            String ticker,
            String name,
            String currency,
            BigDecimal weight,
            BigDecimal securityMovePercent,
            BigDecimal fxMovePercent,
            BigDecimal totalMovePercent,
            BigDecimal weightedContributionPercent
    ) {
        this.ticker = ticker;
        this.name = name;
        this.currency = currency;
        this.weight = weight;
        this.securityMovePercent = securityMovePercent;
        this.fxMovePercent = fxMovePercent;
        this.totalMovePercent = totalMovePercent;
        this.weightedContributionPercent = weightedContributionPercent;
    }

    void attachTo(ScenarioRun scenarioRun) {
        this.scenarioRun = scenarioRun;
    }

    public String getTicker() {
        return ticker;
    }

    public String getName() {
        return name;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public BigDecimal getSecurityMovePercent() {
        return securityMovePercent;
    }

    public BigDecimal getFxMovePercent() {
        return fxMovePercent;
    }

    public BigDecimal getTotalMovePercent() {
        return totalMovePercent;
    }

    public BigDecimal getWeightedContributionPercent() {
        return weightedContributionPercent;
    }
}
