# DRAM Bridge Model

DRAM Bridge Model is a lightweight Spring Boot service for analyzing the Roundhill Memory ETF (`DRAM`) as a temporary bridge investment before rotating into SK hynix and/or Micron.

The current implementation covers Release 0.1 through Release 0.6, plus the initial market data snapshot foundation:

- Spring Boot project skeleton with layered packages.
- Manual DRAM holding snapshot entry.
- Persistence for ETFs, securities, holding snapshots, holdings, and NAV snapshots.
- Synthetic NAV calculation from holding weights, current/prior prices, and FX rates.
- Market price versus synthetic NAV premium/discount.
- Snapshot-to-snapshot attribution with top holding contribution changes.
- Scenario analysis for hypothetical security and FX moves.
- Bridge Score v1 with a rotation signal and recommendation.
- Basic static dashboard served by Spring Boot.
- Source-tagged manual market data snapshots for prices and FX rates.
- Bulk market data import for repeatable local setup.
- DRAM snapshot creation from stored latest market data.
- Basic health endpoint and deterministic unit tests for calculation logic.

## Requirements

- Java 21
- Gradle wrapper included
- MySQL 8 for the `dev` profile

## Running Locally

The default `local` profile uses an in-memory H2 database so the API can run without MySQL during early development:

```bash
./gradlew bootRun
```

By default Spring Boot uses port `8080`, configured in `src/main/resources/application.yml`. If that port is already in use, run on another port:

```bash
./gradlew bootRun --args='--server.port=8081'
```

In IntelliJ, add the same value as a program argument:

```text
--server.port=8081
```

The examples below use a `BASE_URL` variable so the port is explicit. Set it to the port your app actually started on:

```bash
export BASE_URL=http://localhost:8081
```

## Dashboard

The dashboard is served from the Spring Boot app at `/`. If the app is running on port `8081`, open:

```text
http://localhost:8081/
```

If you use another port, replace `8081` with that port. The dashboard uses the same backend APIs documented below, so it needs at least one snapshot before the main values populate. For the easiest local workflow, start with seed data:

```bash
./gradlew bootRun --args='--server.port=8081 --app.seed.enabled=true'
```

With the seed flag enabled, the app creates two local DRAM snapshots if no snapshot exists. Then open the dashboard URL above.

The dashboard supports two snapshot workflows:

- `Snapshot Entry`: paste or edit full manual snapshot JSON, then save it directly.
- `Market Data Workflow`: store price and FX snapshots first, then generate a DRAM snapshot from holding identities and weights.

For the market data workflow, use `Load Sample Market Data` to bulk import deterministic DRAM, SK hynix, Micron, Samsung, ASML, and KRW/USD records. Then use `Generate Snapshot` in the same panel. The generated snapshot becomes the latest dashboard snapshot and powers scenario and bridge score views.

These endpoints also work immediately:

```bash
curl "$BASE_URL/api/dram/latest"
curl "$BASE_URL/api/dram/bridge-score"
```

Health check:

```bash
curl "$BASE_URL/api/health"
```

Spring Actuator health is also available:

```bash
curl "$BASE_URL/actuator/health"
```

## Running With MySQL

Create a local MySQL user/database or allow the configured URL to create the database:

```sql
CREATE DATABASE IF NOT EXISTS dram_bridge;
CREATE USER IF NOT EXISTS 'dram_bridge'@'localhost' IDENTIFIED BY 'dram_bridge';
GRANT ALL PRIVILEGES ON dram_bridge.* TO 'dram_bridge'@'localhost';
```

Run with the `dev` profile:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

Configuration is in `src/main/resources/application-dev.yml`.

## Database Migrations

Schema changes are managed with Flyway migrations in `src/main/resources/db/migration`.

Hibernate is configured with `ddl-auto: validate` for `local`, `test`, and `dev`, so the application verifies that entity mappings match the migrated schema but does not create or alter tables automatically.

If you created a local MySQL `dram_bridge` schema before Flyway was introduced, recreate the dev database before running the app:

```sql
DROP DATABASE IF EXISTS dram_bridge;
CREATE DATABASE dram_bridge;
GRANT ALL PRIVILEGES ON dram_bridge.* TO 'dram_bridge'@'localhost';
```

Then run:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

## API

`GET /api/dram` returns an index of available DRAM API endpoints and the UI path.

### Market Data Snapshots

`POST /api/market-data/prices`

Stores a source-tagged security price snapshot. `observedAt` is optional; when omitted the server uses the current timestamp.

```bash
curl -X POST "$BASE_URL/api/market-data/prices" \
  -H 'Content-Type: application/json' \
  -d '{
    "ticker": "MU",
    "name": "Micron Technology",
    "exchange": "NASDAQ",
    "currency": "USD",
    "price": 108.25,
    "source": "manual"
  }'
```

`POST /api/market-data/fx-rates`

```bash
curl -X POST "$BASE_URL/api/market-data/fx-rates" \
  -H 'Content-Type: application/json' \
  -d '{
    "baseCurrency": "KRW",
    "quoteCurrency": "USD",
    "rate": 0.00081000,
    "source": "manual"
  }'
```

Latest snapshot lookups:

```bash
curl "$BASE_URL/api/market-data/prices/NASDAQ/MU/latest"
curl "$BASE_URL/api/market-data/fx-rates/KRW/USD/latest"
curl "$BASE_URL/api/market-data"
```

Bulk import:

```bash
curl -X POST "$BASE_URL/api/market-data/import" \
  -H 'Content-Type: application/json' \
  -d '{
    "prices": [
      {
        "ticker": "DRAM",
        "name": "Roundhill Memory ETF",
        "exchange": "NYSEARCA",
        "currency": "USD",
        "price": 81.50,
        "source": "manual"
      },
      {
        "ticker": "MU",
        "name": "Micron Technology",
        "exchange": "NASDAQ",
        "currency": "USD",
        "price": 108.25,
        "source": "manual"
      }
    ],
    "fxRates": [
      {
        "baseCurrency": "KRW",
        "quoteCurrency": "USD",
        "rate": 0.00081000,
        "source": "manual"
      }
    ]
  }'
```

### Create DRAM Snapshot From Stored Market Data

`POST /api/dram/snapshot/from-market-data`

This endpoint uses previously stored market data instead of requiring every holding price and FX value inline. The request supplies holding identities and weights. The service looks up:

- DRAM market price from the latest `DRAM` / `NYSEARCA` price snapshot, unless `marketPrice` is provided.
- Current holding price from the latest matching security price snapshot.
- Prior holding price from the previous matching security price snapshot, or the latest value when only one exists.
- Current/prior FX from the latest two `currency` / `USD` FX snapshots, or the latest value when only one exists. USD holdings use `1`.

Example market data setup:

```bash
curl -X POST "$BASE_URL/api/market-data/prices" \
  -H 'Content-Type: application/json' \
  -d '{
    "ticker": "DRAM",
    "name": "Roundhill Memory ETF",
    "exchange": "NYSEARCA",
    "currency": "USD",
    "price": 81.50,
    "source": "manual"
  }'

curl -X POST "$BASE_URL/api/market-data/prices" \
  -H 'Content-Type: application/json' \
  -d '{
    "ticker": "000660",
    "name": "SK hynix",
    "exchange": "KRX",
    "currency": "KRW",
    "price": 114000,
    "source": "manual"
  }'

curl -X POST "$BASE_URL/api/market-data/prices" \
  -H 'Content-Type: application/json' \
  -d '{
    "ticker": "MU",
    "name": "Micron Technology",
    "exchange": "NASDAQ",
    "currency": "USD",
    "price": 108.25,
    "source": "manual"
  }'

curl -X POST "$BASE_URL/api/market-data/fx-rates" \
  -H 'Content-Type: application/json' \
  -d '{
    "baseCurrency": "KRW",
    "quoteCurrency": "USD",
    "rate": 0.00081000,
    "source": "manual"
  }'
```

Then create the DRAM snapshot:

```bash
curl -X POST "$BASE_URL/api/dram/snapshot/from-market-data" \
  -H 'Content-Type: application/json' \
  -d '{
    "asOfDate": "2026-06-26",
    "purchasePrice": 76.31,
    "holdings": [
      {
        "ticker": "000660",
        "name": "SK hynix",
        "exchange": "KRX",
        "currency": "KRW",
        "weight": 0.26
      },
      {
        "ticker": "MU",
        "name": "Micron Technology",
        "exchange": "NASDAQ",
        "currency": "USD",
        "weight": 0.19
      }
    ]
  }'
```

If a required price or FX snapshot is missing, the API returns `404` with a `not_found` error. This is intentional because the calculation should not silently invent market data.

### Create Manual DRAM Snapshot

`POST /api/dram/snapshot`

Weights are decimals, so `0.25` means 25%. FX values are USD per one unit of the holding currency. For USD holdings, use `1`.

```bash
curl -X POST "$BASE_URL/api/dram/snapshot" \
  -H 'Content-Type: application/json' \
  -d '{
    "asOfDate": "2026-06-26",
    "marketPrice": 80.00,
    "purchasePrice": 76.31,
    "holdings": [
      {
        "ticker": "000660",
        "name": "SK hynix",
        "exchange": "KRX",
        "currency": "KRW",
        "weight": 0.25,
        "currentPrice": 110000,
        "priorPrice": 100000,
        "currentFxToUsd": 0.00080,
        "priorFxToUsd": 0.00080
      },
      {
        "ticker": "MU",
        "name": "Micron Technology",
        "exchange": "NASDAQ",
        "currency": "USD",
        "weight": 0.20,
        "currentPrice": 105,
        "priorPrice": 100,
        "currentFxToUsd": 1,
        "priorFxToUsd": 1
      }
    ]
  }'
```

### Get Latest Snapshot

`GET /api/dram/latest`

```bash
curl "$BASE_URL/api/dram/latest"
```

When at least two snapshots exist, responses include an `attribution` object:

```json
{
  "hasPriorSnapshot": true,
  "currentSnapshotId": 2,
  "priorSnapshotId": 1,
  "syntheticNavChangePercent": 2.150000,
  "marketPriceChangePercent": 1.250000,
  "topContributors": [
    {
      "ticker": "000660",
      "name": "SK hynix",
      "currentWeight": 0.250000,
      "priorWeight": 0.240000,
      "currentContributionPercent": 2.500000,
      "priorContributionPercent": 1.200000,
      "contributionChangePercent": 1.300000
    }
  ]
}
```

### Run Scenario

`POST /api/dram/scenario`

This endpoint uses the latest persisted DRAM snapshot as the baseline. Security moves are keyed by holding ticker, and FX moves are keyed by currency. Percent values are entered as whole percentages, so `10` means +10%.

```bash
curl -X POST "$BASE_URL/api/dram/scenario" \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "HBM upside with KRW tailwind",
    "purchasePrice": 76.31,
    "securityMovesPercent": {
      "000660": 10,
      "MU": 5,
      "005930": -3
    },
    "fxMovesPercent": {
      "KRW": 2
    }
  }'
```

Example response fields:

```json
{
  "scenarioRunId": 1,
  "baselineSnapshotId": 2,
  "name": "HBM upside with KRW tailwind",
  "baselineMarketPrice": 80.00,
  "purchasePrice": 76.31,
  "estimatedMovePercent": 3.891000,
  "projectedMarketPrice": 83.112800,
  "dollarImpactVsPurchasePrice": 6.802800
}
```

### Get Bridge Score

`GET /api/dram/bridge-score`

Uses the latest persisted DRAM snapshot. Defaults:

- Target exposure tickers: `000660`, `MU`
- Liquidity placeholder: `70`
- Tracking confidence placeholder: `65`
- Timing risk placeholder: `50`
- Direct SK hynix availability: `false`

```bash
curl "$BASE_URL/api/dram/bridge-score"
```

`POST /api/dram/bridge-score`

Override placeholder assumptions or target tickers:

```bash
curl -X POST "$BASE_URL/api/dram/bridge-score" \
  -H 'Content-Type: application/json' \
  -d '{
    "targetTickers": ["000660", "MU"],
    "liquidityScore": 75,
    "trackingConfidenceScore": 70,
    "timingRiskScore": 55,
    "directSkHynixAvailable": false
  }'
```

Example response fields:

```json
{
  "score": 84.00,
  "rotationSignal": "HOLD DRAM",
  "recommendation": "DRAM remains an efficient bridge based on current exposure and valuation inputs.",
  "targetExposureWeight": 0.450000,
  "premiumDiscountPercent": -1.500000
}
```

## Calculation Formulas

Release 0.2 uses a normalized synthetic NAV model:

```text
local_return = current_price / prior_price - 1
fx_return = current_fx_to_usd / prior_fx_to_usd - 1
total_usd_return = (current_price * current_fx_to_usd) / (prior_price * prior_fx_to_usd) - 1
weighted_contribution = holding_weight * total_usd_return
estimated_etf_move = sum(weighted_contribution)
synthetic_nav = market_price * (1 + estimated_etf_move)
premium_discount = (market_price / synthetic_nav - 1) * 100
```

Attribution compares the latest snapshot to the previous persisted snapshot:

```text
synthetic_nav_change = current_synthetic_nav / prior_synthetic_nav - 1
market_price_change = current_market_price / prior_market_price - 1
contribution_change = current_weighted_contribution - prior_weighted_contribution
```

Scenario analysis uses the latest saved snapshot as the baseline:

```text
scenario_total_move = (1 + security_move) * (1 + fx_move) - 1
scenario_weighted_contribution = holding_weight * scenario_total_move
estimated_dram_move = sum(scenario_weighted_contribution)
projected_market_price = baseline_market_price * (1 + estimated_dram_move)
dollar_impact_vs_purchase = projected_market_price - purchase_price
```

Bridge Score v1 uses a 0-100 weighted score:

```text
target_exposure_score = min((target_exposure_weight / 0.50) * 100, 100)
premium_discount_score =
  100 when discount is at least 1%
  85 when premium/discount is between -1% and 0%
  65 when premium is 0% to 2%
  35 when premium is 2% to 5%
  10 when premium is greater than 5%
bridge_score =
  target_exposure_score * 45%
  + premium_discount_score * 25%
  + liquidity_score * 10%
  + tracking_confidence_score * 10%
  + timing_risk_score * 10%
```

The model distinguishes market price and synthetic NAV. Official NAV and estimated fair value will be added as separate concepts in later releases when official data ingestion exists.

## Testing

```bash
./gradlew test
```

Tests use the `test` profile in `src/test/resources/application-test.yml`, backed by in-memory H2 in MySQL compatibility mode. Flyway applies the same versioned migrations used by local/dev startup, then Hibernate validates the mappings. API integration tests seed data through the public snapshot endpoint using deterministic fixtures in `src/test/java/com/temadison/drambuilder/fixtures`.

Static UI resources are served from `src/main/resources/static` and covered by integration tests.

Current tests cover:

- Synthetic NAV calculation.
- Premium/discount calculation.
- FX-adjusted holding contribution.
- Snapshot-to-snapshot attribution and top contributor ranking.
- Scenario sensitivity and dollar impact calculations.
- Bridge Score v1 and rotation signal selection.
- API integration coverage for latest snapshot, scenario, bridge score, and missing snapshot behavior.
- Market-data-driven snapshot creation through public API endpoints.
- Bulk market data import.
- Static dashboard resource coverage, including the market data workflow.
- Invalid total holding weights.

## Architecture

See [docs/architecture.md](docs/architecture.md).
