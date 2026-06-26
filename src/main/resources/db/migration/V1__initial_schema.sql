CREATE TABLE etf (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ticker VARCHAR(16) NOT NULL,
    name VARCHAR(160) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_etf_ticker UNIQUE (ticker)
);

CREATE TABLE security (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ticker VARCHAR(32) NOT NULL,
    name VARCHAR(160) NOT NULL,
    exchange VARCHAR(32) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_security_ticker_exchange UNIQUE (ticker, exchange)
);

CREATE TABLE etf_holding_snapshot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    etf_id BIGINT NOT NULL,
    as_of_date DATE NOT NULL,
    market_price DECIMAL(18, 6) NOT NULL,
    purchase_price DECIMAL(18, 6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_holding_snapshot_etf FOREIGN KEY (etf_id) REFERENCES etf (id)
);

CREATE INDEX idx_holding_snapshot_etf_created_at
    ON etf_holding_snapshot (etf_id, created_at);

CREATE TABLE etf_holding (
    id BIGINT NOT NULL AUTO_INCREMENT,
    snapshot_id BIGINT NOT NULL,
    security_id BIGINT NOT NULL,
    weight DECIMAL(10, 6) NOT NULL,
    current_price DECIMAL(18, 6) NOT NULL,
    prior_price DECIMAL(18, 6) NOT NULL,
    current_fx_to_usd DECIMAL(18, 8) NOT NULL,
    prior_fx_to_usd DECIMAL(18, 8) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_etf_holding_snapshot FOREIGN KEY (snapshot_id) REFERENCES etf_holding_snapshot (id),
    CONSTRAINT fk_etf_holding_security FOREIGN KEY (security_id) REFERENCES security (id)
);

CREATE INDEX idx_etf_holding_snapshot
    ON etf_holding (snapshot_id);

CREATE TABLE nav_snapshot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    holding_snapshot_id BIGINT NOT NULL,
    synthetic_nav DECIMAL(18, 6) NOT NULL,
    market_price DECIMAL(18, 6) NOT NULL,
    premium_discount_percent DECIMAL(10, 6) NOT NULL,
    estimated_etf_move_percent DECIMAL(10, 6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_nav_snapshot_holding_snapshot UNIQUE (holding_snapshot_id),
    CONSTRAINT fk_nav_snapshot_holding_snapshot FOREIGN KEY (holding_snapshot_id) REFERENCES etf_holding_snapshot (id)
);

CREATE INDEX idx_nav_snapshot_created_at
    ON nav_snapshot (created_at);

CREATE TABLE scenario_run (
    id BIGINT NOT NULL AUTO_INCREMENT,
    holding_snapshot_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    purchase_price DECIMAL(18, 6) NOT NULL,
    estimated_move_percent DECIMAL(10, 6) NOT NULL,
    projected_market_price DECIMAL(18, 6) NOT NULL,
    dollar_impact_vs_purchase_price DECIMAL(18, 6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_scenario_run_holding_snapshot FOREIGN KEY (holding_snapshot_id) REFERENCES etf_holding_snapshot (id)
);

CREATE INDEX idx_scenario_run_snapshot_created_at
    ON scenario_run (holding_snapshot_id, created_at);

CREATE TABLE scenario_result (
    id BIGINT NOT NULL AUTO_INCREMENT,
    scenario_run_id BIGINT NOT NULL,
    display_order INT NULL,
    ticker VARCHAR(32) NOT NULL,
    name VARCHAR(160) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    weight DECIMAL(10, 6) NOT NULL,
    security_move_percent DECIMAL(10, 6) NOT NULL,
    fx_move_percent DECIMAL(10, 6) NOT NULL,
    total_move_percent DECIMAL(10, 6) NOT NULL,
    weighted_contribution_percent DECIMAL(10, 6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_scenario_result_run FOREIGN KEY (scenario_run_id) REFERENCES scenario_run (id)
);

CREATE INDEX idx_scenario_result_run_order
    ON scenario_result (scenario_run_id, display_order);
