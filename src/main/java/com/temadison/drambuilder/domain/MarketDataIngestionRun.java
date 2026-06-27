package com.temadison.drambuilder.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "market_data_ingestion_run")
public class MarketDataIngestionRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String source;

    @Column(nullable = false, length = 24)
    private String status;

    @Column(length = 512)
    private String requestedFile;

    @Column(nullable = false)
    private int pricesImported;

    @Column(nullable = false)
    private int fxRatesImported;

    @Column(nullable = false)
    private int officialNavsImported;

    @Column(nullable = false)
    private boolean snapshotCreated;

    @Column(length = 1000)
    private String message;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant completedAt;

    protected MarketDataIngestionRun() {
    }

    public MarketDataIngestionRun(String source, String requestedFile, Instant startedAt) {
        this.source = source;
        this.status = "RUNNING";
        this.requestedFile = requestedFile;
        this.pricesImported = 0;
        this.fxRatesImported = 0;
        this.officialNavsImported = 0;
        this.snapshotCreated = false;
        this.startedAt = startedAt;
    }

    public void complete(int pricesImported, int fxRatesImported, int officialNavsImported, boolean snapshotCreated, String message, Instant completedAt) {
        this.status = "SUCCESS";
        this.pricesImported = pricesImported;
        this.fxRatesImported = fxRatesImported;
        this.officialNavsImported = officialNavsImported;
        this.snapshotCreated = snapshotCreated;
        this.message = truncate(message);
        this.completedAt = completedAt;
    }

    public void fail(String message, Instant completedAt) {
        this.status = "FAILED";
        this.message = truncate(message);
        this.completedAt = completedAt;
    }

    public Long getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getStatus() {
        return status;
    }

    public String getRequestedFile() {
        return requestedFile;
    }

    public int getPricesImported() {
        return pricesImported;
    }

    public int getFxRatesImported() {
        return fxRatesImported;
    }

    public int getOfficialNavsImported() {
        return officialNavsImported;
    }

    public boolean isSnapshotCreated() {
        return snapshotCreated;
    }

    public String getMessage() {
        return message;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 1000) {
            return value;
        }
        return value.substring(0, 1000);
    }
}
