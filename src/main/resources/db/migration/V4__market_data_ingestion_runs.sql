CREATE TABLE market_data_ingestion_run (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source VARCHAR(80) NOT NULL,
    status VARCHAR(24) NOT NULL,
    requested_file VARCHAR(512) NULL,
    prices_imported INT NOT NULL,
    fx_rates_imported INT NOT NULL,
    official_navs_imported INT NOT NULL,
    snapshot_created BOOLEAN NOT NULL,
    message VARCHAR(1000) NULL,
    started_at TIMESTAMP(6) NOT NULL,
    completed_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_market_data_ingestion_run_started_at
    ON market_data_ingestion_run (started_at);

CREATE INDEX idx_market_data_ingestion_run_status_started_at
    ON market_data_ingestion_run (status, started_at);
