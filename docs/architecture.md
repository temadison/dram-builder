# Architecture Overview

## Release Scope

This repository currently implements Release 0.1 through Release 0.6.

Release 0.1 establishes the Spring Boot skeleton, layered package layout, health endpoint, configuration, and test framework.

Release 0.2 adds manual snapshot ingestion, persistence, synthetic NAV calculation, and latest snapshot retrieval.

Release 0.3 adds snapshot-to-snapshot attribution by comparing the latest snapshot with the prior persisted snapshot and ranking holding contribution changes.

Release 0.4 adds scenario analysis for hypothetical security and FX moves against the latest persisted snapshot.

Release 0.5 adds Bridge Score v1 and a rotation signal based on target exposure, premium/discount, and explicit placeholder quality scores.

Release 0.6 adds a basic static dashboard served by Spring Boot for latest snapshot, holdings, attribution, scenarios, bridge score, and manual snapshot entry.

The current post-0.6 step connects stored market data to snapshot creation so manual price/FX entry can be separated from DRAM NAV calculation.

## Package Layout

- `domain`: JPA entities for ETF, security, holding snapshots, holdings, and NAV snapshots.
- `repository`: Spring Data repositories. These should remain persistence-focused.
- `service`: Calculation and orchestration services. Financial math belongs here.
- `controller`: REST endpoints and API error handling.
- `dto`: Request and response contracts.
- `config`: Application configuration.
- `static`: Lightweight browser UI using ES modules and plain CSS.

## Data Model

Implemented tables:

- `etf`
- `security`
- `etf_holding_snapshot`
- `etf_holding`
- `nav_snapshot`
- `scenario_run`
- `scenario_result`
- `price_snapshot`
- `fx_rate_snapshot`

Planned tables:

- provider ingestion run tables
- official NAV snapshot tables

## Migrations

Flyway owns schema creation and evolution. Migration files live in `src/main/resources/db/migration`.

Hibernate uses `ddl-auto: validate` in local, test, and dev profiles. This keeps entity mappings honest without allowing Hibernate to mutate database structure implicitly.

The initial migration `V1__initial_schema.sql` creates the current ETF, security, holdings, NAV, and scenario tables. Future schema changes should be added as new versioned migrations rather than editing existing applied migrations.

`V2__market_data_snapshots.sql` adds source-tagged security price and FX rate snapshots. These tables are intentionally provider-neutral so manual entry, CSV import, or automated provider ingestion can all write the same normalized records.

## Domain Boundaries

`SyntheticNavCalculator`, `AttributionCalculator`, `ScenarioCalculator`, and `BridgeScoreCalculator` are the calculation contracts. Their default Spring implementations are persistence-free and covered by deterministic unit tests.

`SyntheticNavService` calculates normalized synthetic NAV, holding returns, and premium/discount.

`AttributionService` ranks holding-level contribution changes and calculates synthetic NAV and market price changes versus the prior snapshot.

`ScenarioService` applies security and FX moves to current holding weights and returns estimated DRAM move, projected price, and dollar impact versus purchase price.

`BridgeScoreService` calculates Bridge Score v1 and chooses a rotation signal from `HOLD DRAM`, `ROTATE TO SK HYNIX`, `WAIT`, and `AVOID ADDING`.

`DramSnapshotService` coordinates DRAM-specific snapshot ingestion, entity persistence, NAV calculation, attribution, and snapshot response mapping.

`DramMarketDataSnapshotService` converts latest stored security prices and FX rates into the existing snapshot input contract, then delegates persistence and NAV math to `DramSnapshotService`.

`DramScenarioService` coordinates scenario execution against the latest snapshot and persists scenario runs and holding-level scenario results.

`DramBridgeScoreService` builds score inputs from the latest snapshot and applies default or request-provided placeholder assumptions.

`MarketDataService` stores manual security prices and FX rates with source and observed timestamp metadata. Automated data providers should either call this service or implement a provider-specific ingestion service that writes the same tables.

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
- `test`: isolated in-memory H2 profile for Spring integration tests.
- `dev`: MySQL-backed profile for persistent development data.

## Test Data

Unit tests use deterministic object fixtures and avoid persistence for calculation services.

API integration tests use `@SpringBootTest`, `MockMvc`, the `test` profile, and fixture-driven snapshot creation through `POST /api/dram/snapshot`. This verifies controller, validation, service, repository, and JPA behavior together without opening a real server port.

Market-data-driven snapshot tests seed price and FX records through `/api/market-data/*`, then create a snapshot through `/api/dram/snapshot/from-market-data`. This verifies the intended developer workflow without direct repository setup.

Manual local seed data is opt-in with `app.seed.enabled=true` under the `local` profile. The `LocalSeedDataRunner` creates two sample DRAM snapshots only when no snapshot exists, allowing `/api/dram/latest`, `/api/dram/scenario`, and `/api/dram/bridge-score` to be exercised immediately after startup.

## UI

The Release 0.6 UI is intentionally static and build-free. `index.html` loads ES modules from `static/js`:

- `api.js`: REST calls.
- `format.js`: display formatting helpers.
- `sampleData.js`: deterministic manual-entry sample snapshot.
- `view.js`: DOM rendering.
- `app.js`: UI orchestration and event handling.

The dashboard is served at `/`, and `GET /api/dram` returns an API index for manual discovery.

## Next Release

Next releases should improve data management and production hardening, including CSV/provider ingestion, official NAV capture, richer validation, and dashboard support for stored market data.
