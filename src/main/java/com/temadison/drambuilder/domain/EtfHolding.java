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
@Table(name = "etf_holding")
public class EtfHolding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private EtfHoldingSnapshot snapshot;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "security_id", nullable = false)
    private Security security;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal weight;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal currentPrice;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal priorPrice;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal currentFxToUsd;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal priorFxToUsd;

    protected EtfHolding() {
    }

    public EtfHolding(Security security, BigDecimal weight, BigDecimal currentPrice, BigDecimal priorPrice, BigDecimal currentFxToUsd, BigDecimal priorFxToUsd) {
        this.security = security;
        this.weight = weight;
        this.currentPrice = currentPrice;
        this.priorPrice = priorPrice;
        this.currentFxToUsd = currentFxToUsd;
        this.priorFxToUsd = priorFxToUsd;
    }

    void attachTo(EtfHoldingSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public Long getId() {
        return id;
    }

    public Security getSecurity() {
        return security;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public BigDecimal getPriorPrice() {
        return priorPrice;
    }

    public BigDecimal getCurrentFxToUsd() {
        return currentFxToUsd;
    }

    public BigDecimal getPriorFxToUsd() {
        return priorFxToUsd;
    }
}
