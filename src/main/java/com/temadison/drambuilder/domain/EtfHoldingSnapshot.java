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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "etf_holding_snapshot")
public class EtfHoldingSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "etf_id", nullable = false)
    private Etf etf;

    @Column(nullable = false)
    private LocalDate asOfDate;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal marketPrice;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal purchasePrice;

    @Column(nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EtfHolding> holdings = new ArrayList<>();

    protected EtfHoldingSnapshot() {
    }

    public EtfHoldingSnapshot(Etf etf, LocalDate asOfDate, BigDecimal marketPrice, BigDecimal purchasePrice, Instant createdAt) {
        this.etf = etf;
        this.asOfDate = asOfDate;
        this.marketPrice = marketPrice;
        this.purchasePrice = purchasePrice;
        this.createdAt = createdAt;
    }

    public void addHolding(EtfHolding holding) {
        holdings.add(holding);
        holding.attachTo(this);
    }

    public Long getId() {
        return id;
    }

    public Etf getEtf() {
        return etf;
    }

    public LocalDate getAsOfDate() {
        return asOfDate;
    }

    public BigDecimal getMarketPrice() {
        return marketPrice;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<EtfHolding> getHoldings() {
        return holdings;
    }
}
