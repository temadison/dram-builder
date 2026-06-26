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
import java.time.Instant;

@Entity
@Table(name = "price_snapshot")
public class PriceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "security_id", nullable = false)
    private Security security;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 80)
    private String source;

    @Column(nullable = false)
    private Instant observedAt;

    @Column(nullable = false)
    private Instant createdAt;

    protected PriceSnapshot() {
    }

    public PriceSnapshot(Security security, BigDecimal price, String currency, String source, Instant observedAt, Instant createdAt) {
        this.security = security;
        this.price = price;
        this.currency = currency;
        this.source = source;
        this.observedAt = observedAt;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Security getSecurity() {
        return security;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
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
