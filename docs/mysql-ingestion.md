# MySQL and Market Data Ingestion Setup

This guide connects the app to local MySQL and loads market data from a JSON ingestion file. Use this for persistent development data and for repeatable loads from a provider-generated export.

## Start MySQL

```bash
docker compose up -d mysql
```

The compose file creates:

- database: `dram_bridge`
- user: `dram_bridge`
- password: `dram_bridge`
- default host port: `3306`

The Spring `dev` profile is already configured for those values in `src/main/resources/application-dev.yml`. Flyway applies all migrations on startup.

If port `3306` is already in use, choose another host port:

```bash
DRAM_MYSQL_PORT=3307 docker compose up -d mysql
```

When using a non-default host port, pass a matching datasource URL to Spring:

```bash
--spring.datasource.url=jdbc:mysql://localhost:3307/dram_bridge?createDatabaseIfNotExist=true\&useSSL=false\&allowPublicKeyRetrieval=true\&serverTimezone=UTC
```

## Prepare Ingestion Data

Create a JSON file using `docs/dev-market-data.example.json` as the shape. The example is not a current market data source; replace its prices, FX rates, official NAV, dates, and holdings with values from your provider.

`docs/dev-market-data-2026-06-01.json` is a partial sourced starter file for June 1, 2026. It uses StockAnalysis/S&P Global historical closes for DRAM, MU, SNDK, WDC, and STX, and Roundhill's published holdings weights for those U.S.-listed DRAM holdings. It intentionally omits SK hynix, Samsung, Kioxia, Nanya, and Winbond until a provider with Korea/Japan/Taiwan coverage is configured.

The ingestion file supports:

- `prices`: security or ETF price snapshots.
- `fxRates`: FX rates to USD.
- `officialNavs`: issuer/provider ETF NAV snapshots.
- `snapshot`: optional DRAM snapshot generation from the stored price and FX records.

For current DRAM setup, use Roundhill as the issuer source for holdings and official NAV, then use a market data provider for live or prior-close prices and FX. Good provider candidates are Twelve Data, Polygon, Tiingo, or Alpha Vantage, depending on international equity coverage for Korea, Japan, and Taiwan.

## One-Shot Load Into MySQL

Run this after MySQL is healthy:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun --args='--app.ingest.enabled=true --app.ingest.file=file:/absolute/path/to/market-data.json --app.ingest.exit-after-run=true'
```

If MySQL is on a non-default host port, include the datasource override in the same `--args` string.

The runner validates the file, stores price/FX/NAV snapshots through the same service paths used by the API, optionally creates a DRAM snapshot, and exits when `app.ingest.exit-after-run=true`.

## Run the App Against MySQL

After loading data:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun --args='--server.port=8082'
```

Then open:

```text
http://localhost:8082/
```

Useful checks:

```bash
curl http://localhost:8082/api/market-data
curl http://localhost:8082/api/market-data/ingestion-runs
curl http://localhost:8082/api/dram/latest
curl http://localhost:8082/api/dram/bridge-score
```

Every file ingestion run writes a `market_data_ingestion_run` record. Recent runs are available at `/api/market-data/ingestion-runs` with status, source, requested file, imported row counts, snapshot creation status, and timing.

## Remaining Provider Automation

The app now has a repeatable MySQL load path, but it does not yet fetch live provider APIs itself. To fully automate recent values, add a provider adapter that writes this ingestion file or calls `MarketDataService` directly.

## Recommended Refresh Cadence

DRAM holds securities across U.S. and Asia-Pacific markets. For a Central Time workstation, the useful refresh windows are the gaps after one region has closed and before the other region opens:

- `02:00 America/Chicago`: after Korea/Japan/Taiwan regular sessions have closed during U.S. daylight time, and before the U.S. regular session opens.
- `16:30 America/Chicago`: after the U.S. regular session has closed, and before Korea/Japan/Taiwan regular sessions open.

These should be scheduler triggers, not hard-coded data assumptions. A provider-backed job should still check exchange holidays, early closes, and whether all expected quotes/NAV records are available before creating a new DRAM snapshot.

Recommended next implementation:

1. Pick one provider for prices and FX.
2. Add API-key config under `app.provider`.
3. Add a provider client that resolves DRAM holdings, prices, FX, and official NAV.
4. Map provider records into `MarketDataIngestionRequest`.
5. Schedule the provider job for `02:00` and `16:30` Central Time.
