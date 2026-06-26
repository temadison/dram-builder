# Architecture Overview

## Release Scope

This repository currently implements Release 0.1 through Release 0.4.

Release 0.1 establishes the Spring Boot skeleton, layered package layout, health endpoint, configuration, and test framework.

Release 0.2 adds manual snapshot ingestion, persistence, synthetic NAV calculation, and latest snapshot retrieval.

Release 0.3 adds snapshot-to-snapshot attribution by comparing the latest snapshot with the prior persisted snapshot and ranking holding contribution changes.

Release 0.4 adds scenario analysis for hypothetical security and FX moves against the latest persisted snapshot.

## Package Layout

- `domain`: JPA entities for ETF, security, holding snapshots, holdings, and NAV snapshots.
- `repository`: Spring Data repositories. These should remain persistence-focused.
- `service`: Calculation and orchestration services. Financial math belongs here.
- `controller`: REST endpoints and API error handling.
- `dto`: Request and response contracts.
- `config`: Application configuration.

## Data Model

Implemented tables:

- `etf`
- `security`
- `etf_holding_snapshot`
- `etf_holding`
- `nav_snapshot`
- `scenario_run`
- `scenario_result`

Planned tables:

- `price_snapshot`
- `fx_rate_snapshot`

## Domain Boundaries

`SyntheticNavService` is the pure calculation service. It does not depend on Spring persistence and is covered by deterministic unit tests.

`AttributionService` is the pure snapshot comparison service. It ranks holding-level contribution changes and calculates synthetic NAV and market price changes versus the prior snapshot.

`ScenarioService` is the pure hypothetical move service. It applies security and FX moves to current holding weights and returns estimated DRAM move, projected price, and dollar impact versus purchase price.

`DramSnapshotService` coordinates DRAM-specific snapshot ingestion, entity persistence, calculation, and DTO mapping. Future releases should extract generic ETF snapshot handling when additional bridge trades or ETFs are supported.

## Financial Concepts

The current model explicitly tracks:

- Market price: the actual DRAM trade price supplied by the user.
- Synthetic NAV: normalized estimate from current/prior holding prices and FX rates.
- Premium/discount: market price relative to synthetic NAV.

Planned concepts:

- Official NAV from issuer or market data provider.
- Estimated fair value from richer holdings, cash, fees, stale quote logic, and market timing adjustments.
- Bridge score and rotation signal.

## Profiles

- `local`: default profile using in-memory H2 for lightweight development.
- `dev`: MySQL-backed profile for persistent development data.

## Next Release

Release 0.5 should add Bridge Score v1 with exposure, premium/discount, liquidity placeholder, tracking confidence placeholder, event timing risk placeholder, and a plain-English recommendation.
