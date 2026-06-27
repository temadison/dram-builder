CREATE TABLE official_nav_snapshot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    etf_id BIGINT NOT NULL,
    nav DECIMAL(18, 6) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    source VARCHAR(80) NOT NULL,
    as_of_date DATE NOT NULL,
    observed_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_official_nav_snapshot_etf FOREIGN KEY (etf_id) REFERENCES etf (id)
);

CREATE INDEX idx_official_nav_snapshot_etf_observed_at
    ON official_nav_snapshot (etf_id, observed_at);

CREATE INDEX idx_official_nav_snapshot_etf_as_of_date
    ON official_nav_snapshot (etf_id, as_of_date);
