package com.temadison.drambuilder.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "nav_snapshot")
public class NavSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "holding_snapshot_id", nullable = false, unique = true)
    private EtfHoldingSnapshot holdingSnapshot;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal syntheticNav;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal marketPrice;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal premiumDiscountPercent;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal estimatedEtfMovePercent;

    @Column(nullable = false)
    private Instant createdAt;

    protected NavSnapshot() {
    }

    public NavSnapshot(EtfHoldingSnapshot holdingSnapshot, BigDecimal syntheticNav, BigDecimal marketPrice, BigDecimal premiumDiscountPercent, BigDecimal estimatedEtfMovePercent, Instant createdAt) {
        this.holdingSnapshot = holdingSnapshot;
        this.syntheticNav = syntheticNav;
        this.marketPrice = marketPrice;
        this.premiumDiscountPercent = premiumDiscountPercent;
        this.estimatedEtfMovePercent = estimatedEtfMovePercent;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public EtfHoldingSnapshot getHoldingSnapshot() {
        return holdingSnapshot;
    }

    public BigDecimal getSyntheticNav() {
        return syntheticNav;
    }

    public BigDecimal getMarketPrice() {
        return marketPrice;
    }

    public BigDecimal getPremiumDiscountPercent() {
        return premiumDiscountPercent;
    }

    public BigDecimal getEstimatedEtfMovePercent() {
        return estimatedEtfMovePercent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
