package com.temadison.drambuilder.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fx_rate_snapshot")
public class FxRateSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 3)
    private String baseCurrency;

    @Column(nullable = false, length = 3)
    private String quoteCurrency;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal rate;

    @Column(nullable = false, length = 80)
    private String source;

    @Column(nullable = false)
    private Instant observedAt;

    @Column(nullable = false)
    private Instant createdAt;

    protected FxRateSnapshot() {
    }

    public FxRateSnapshot(String baseCurrency, String quoteCurrency, BigDecimal rate, String source, Instant observedAt, Instant createdAt) {
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
        this.rate = rate;
        this.source = source;
        this.observedAt = observedAt;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public String getQuoteCurrency() {
        return quoteCurrency;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public String getSource() {
        return source;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
