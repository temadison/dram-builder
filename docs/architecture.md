# Architecture Overview

## Release Scope

This repository currently implements Release 0.1 through Release 0.5.

Release 0.1 establishes the Spring Boot skeleton, layered package layout, health endpoint, configuration, and test framework.

Release 0.2 adds manual snapshot ingestion, persistence, synthetic NAV calculation, and latest snapshot retrieval.

Release 0.3 adds snapshot-to-snapshot attribution by comparing the latest snapshot with the prior persisted snapshot and ranking holding contribution changes.

Release 0.4 adds scenario analysis for hypothetical security and FX moves against the latest persisted snapshot.

Release 0.5 adds Bridge Score v1 and a rotation signal based on target exposure, premium/discount, and explicit placeholder quality scores.

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

`SyntheticNavCalculator`, `AttributionCalculator`, `ScenarioCalculator`, and `BridgeScoreCalculator` are the calculation contracts. Their default Spring implementations are persistence-free and covered by deterministic unit tests.

`SyntheticNavService` calculates normalized synthetic NAV, holding returns, and premium/discount.

`AttributionService` ranks holding-level contribution changes and calculates synthetic NAV and market price changes versus the prior snapshot.

`ScenarioService` applies security and FX moves to current holding weights and returns estimated DRAM move, projected price, and dollar impact versus purchase price.

`BridgeScoreService` calculates Bridge Score v1 and chooses a rotation signal from `HOLD DRAM`, `ROTATE TO SK HYNIX`, `WAIT`, and `AVOID ADDING`.

`DramSnapshotService` coordinates DRAM-specific snapshot ingestion, entity persistence, NAV calculation, attribution, and snapshot response mapping.

`DramScenarioService` coordinates scenario execution against the latest snapshot and persists scenario runs and holding-level scenario results.

`DramBridgeScoreService` builds score inputs from the latest snapshot and applies default or request-provided placeholder assumptions.

Future releases should extract generic ETF application services when additional bridge trades or ETFs are supported.

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

Release 0.6 should add a basic modular UI for latest snapshot, holdings, sensitivities, bridge score, and recommendation.
