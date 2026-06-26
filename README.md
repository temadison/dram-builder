# DRAM Bridge Model

DRAM Bridge Model is a lightweight Spring Boot service for analyzing the Roundhill Memory ETF (`DRAM`) as a temporary bridge investment before rotating into SK hynix and/or Micron.

The current implementation covers Release 0.1 and Release 0.2:

- Spring Boot project skeleton with layered packages.
- Manual DRAM holding snapshot entry.
- Persistence for ETFs, securities, holding snapshots, holdings, and NAV snapshots.
- Synthetic NAV calculation from holding weights, current/prior prices, and FX rates.
- Market price versus synthetic NAV premium/discount.
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

Health check:

```bash
curl http://localhost:8080/api/health
```

Spring Actuator health is also available:

```bash
curl http://localhost:8080/actuator/health
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

## API

### Create Manual DRAM Snapshot

`POST /api/dram/snapshot`

Weights are decimals, so `0.25` means 25%. FX values are USD per one unit of the holding currency. For USD holdings, use `1`.

```bash
curl -X POST http://localhost:8080/api/dram/snapshot \
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
curl http://localhost:8080/api/dram/latest
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

The model distinguishes market price and synthetic NAV. Official NAV and estimated fair value will be added as separate concepts in later releases when official data ingestion exists.

## Testing

```bash
./gradlew test
```

Current tests cover:

- Synthetic NAV calculation.
- Premium/discount calculation.
- FX-adjusted holding contribution.
- Invalid total holding weights.

## Architecture

See [docs/architecture.md](docs/architecture.md).
