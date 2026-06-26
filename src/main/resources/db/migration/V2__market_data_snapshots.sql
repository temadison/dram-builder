CREATE TABLE price_snapshot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    security_id BIGINT NOT NULL,
    price DECIMAL(18, 6) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    source VARCHAR(80) NOT NULL,
    observed_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_price_snapshot_security FOREIGN KEY (security_id) REFERENCES security (id)
);

CREATE INDEX idx_price_snapshot_security_observed_at
    ON price_snapshot (security_id, observed_at);

CREATE TABLE fx_rate_snapshot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    base_currency VARCHAR(3) NOT NULL,
    quote_currency VARCHAR(3) NOT NULL,
    rate DECIMAL(18, 8) NOT NULL,
    source VARCHAR(80) NOT NULL,
    observed_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_fx_rate_snapshot_pair_observed_at
    ON fx_rate_snapshot (base_currency, quote_currency, observed_at);
