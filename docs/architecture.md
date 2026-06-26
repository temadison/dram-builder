# Architecture Overview

## Release Scope

This repository currently implements Release 0.1 and Release 0.2 only.

Release 0.1 establishes the Spring Boot skeleton, layered package layout, health endpoint, configuration, and test framework.

Release 0.2 adds manual snapshot ingestion, persistence, synthetic NAV calculation, and latest snapshot retrieval.

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

Planned tables:

- `price_snapshot`
- `fx_rate_snapshot`
- `scenario_run`
- `scenario_result`

## Domain Boundaries

`SyntheticNavService` is the pure calculation service. It does not depend on Spring persistence and is covered by deterministic unit tests.

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

Release 0.3 should add attribution over time by comparing the latest snapshot to the prior snapshot and returning top contributors to ETF movement.
