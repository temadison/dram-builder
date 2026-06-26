package com.temadison.drambuilder.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "security", uniqueConstraints = @UniqueConstraint(name = "uk_security_ticker_exchange", columnNames = {"ticker", "exchange"}))
public class Security {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String ticker;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 32)
    private String exchange;

    @Column(nullable = false, length = 3)
    private String currency;

    protected Security() {
    }

    public Security(String ticker, String name, String exchange, String currency) {
        this.ticker = ticker;
        this.name = name;
        this.exchange = exchange;
        this.currency = currency;
    }

    public Long getId() {
        return id;
    }

    public String getTicker() {
        return ticker;
    }

    public String getName() {
        return name;
    }

    public String getExchange() {
        return exchange;
    }

    public String getCurrency() {
        return currency;
    }
}
