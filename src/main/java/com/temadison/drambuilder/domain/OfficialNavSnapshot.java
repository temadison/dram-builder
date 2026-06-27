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
import java.time.LocalDate;

@Entity
@Table(name = "official_nav_snapshot")
public class OfficialNavSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "etf_id", nullable = false)
    private Etf etf;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal nav;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 80)
    private String source;

    @Column(nullable = false)
    private LocalDate asOfDate;

    @Column(nullable = false)
    private Instant observedAt;

    @Column(nullable = false)
    private Instant createdAt;

    protected OfficialNavSnapshot() {
    }

    public OfficialNavSnapshot(Etf etf, BigDecimal nav, String currency, String source, LocalDate asOfDate, Instant observedAt, Instant createdAt) {
        this.etf = etf;
        this.nav = nav;
        this.currency = currency;
        this.source = source;
        this.asOfDate = asOfDate;
        this.observedAt = observedAt;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Etf getEtf() {
        return etf;
    }

    public BigDecimal getNav() {
        return nav;
    }

    public String getCurrency() {
        return currency;
    }

    public String getSource() {
        return source;
    }

    public LocalDate getAsOfDate() {
        return asOfDate;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
