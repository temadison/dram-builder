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

Keep mutable local ingestion files under `data/ingest/`. JSON files in that directory are ignored by git so provider exports and local corrections do not become committed source files. Recommended local filename:

```text
data/ingest/dram-market-data-local.json
```

Start from a committed example:

```bash
cp docs/dev-market-data-2026-06-01.json data/ingest/dram-market-data-local.json
```

The ingestion file supports:

- `prices`: security or ETF price snapshots.
- `fxRates`: FX rates to USD.
- `officialNavs`: issuer/provider ETF NAV snapshots.
- `snapshot`: optional DRAM snapshot generation from the stored price and FX records.

For current DRAM setup, use Roundhill as the issuer source for holdings and official NAV, then use Twelve Data as the first automated provider for live or prior-close prices and FX. See `docs/provider-selection.md` for the provider decision, assumptions, and symbol map.

## One-Shot Load Into MySQL

Run this after MySQL is healthy:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun --args='--app.ingest.enabled=true --app.ingest.file=file:/absolute/path/to/market-data.json --app.ingest.exit-after-run=true'
```

For this repository's default local path:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun --args='--app.ingest.enabled=true --app.ingest.file=file:/Users/temadison/Development/Personal/GitHub/dram-builder/data/ingest/dram-market-data-local.json --app.ingest.exit-after-run=true'
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
curl http://localhost:8082/api/market-data/ingestion-config
curl http://localhost:8082/api/market-data/ingestion-runs
curl http://localhost:8082/api/dram/latest
curl http://localhost:8082/api/dram/bridge-score
```

`/api/market-data/ingestion-config` returns non-secret runtime settings for the current app process. Use it to confirm IntelliJ or command-line flags such as `app.ingest.file`, `app.ingest.schedule.enabled`, `app.ingest.schedule.mode`, and the two cron windows.

Every file ingestion run writes a `market_data_ingestion_run` record. Recent runs are available at `/api/market-data/ingestion-runs` with status, source, requested file, imported row counts, snapshot creation status, and timing.

The data page at `/data.html` also shows recent ingestion runs, so scheduled file/provider failures are visible without querying the API directly.

`GET /api/market-data` includes a `freshness` block for the configured required price set. The default dev set is `BATS:DRAM,NASDAQ:MU,NASDAQ:SNDK,NASDAQ:WDC,NASDAQ:STX`; adjust `app.market-data.freshness.required-prices` when a provider can load Korea, Japan, and Taiwan holdings. `app.market-data.freshness.max-age-hours` controls when an observed price becomes stale.

## Remaining Provider Automation

The app now has a repeatable MySQL load path, but it does not yet fetch live provider APIs itself. To fully automate recent values, add a provider adapter that writes this ingestion file or calls `MarketDataService` directly.

## Recommended Refresh Cadence

DRAM holds securities across U.S. and Asia-Pacific markets. For a Central Time workstation, the useful refresh windows are the gaps after one region has closed and before the other region opens:

- `02:00 America/Chicago`: after Korea/Japan/Taiwan regular sessions have closed during U.S. daylight time, and before the U.S. regular session opens.
- `16:30 America/Chicago`: after the U.S. regular session has closed, and before Korea/Japan/Taiwan regular sessions open.

These should be scheduler triggers, not hard-coded data assumptions. A provider-backed job should still check exchange holidays, early closes, and whether all expected quotes/NAV records are available before creating a new DRAM snapshot.

The app includes a disabled-by-default file scheduler using those windows. It reuses the JSON ingestion file path and records each scheduled attempt in `/api/market-data/ingestion-runs`.

To enable scheduled file ingestion against the Docker MySQL database:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun --args='--spring.datasource.url=jdbc:mysql://localhost:3307/dram_bridge?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC --server.port=8082 --app.ingest.file=file:/absolute/path/to/market-data.json --app.ingest.schedule.enabled=true'
```

For IntelliJ, use the same `data/ingest/dram-market-data-local.json` path in `--app.ingest.file`.

Default schedule:

- `app.ingest.schedule.mode`: `file`
- `app.ingest.schedule.morning-cron`: `0 0 2 * * MON-FRI`
- `app.ingest.schedule.evening-cron`: `0 30 16 * * MON-FRI`
- `app.ingest.schedule.zone`: `America/Chicago`

Provider mode is scaffolded but not connected to a vendor yet:

```bash
--app.ingest.schedule.mode=provider
```

Until a `MarketDataProvider` implementation is configured, provider mode records a failed ingestion run with `No market data provider is configured`. This is intentional so scheduler wiring can be verified before provider credentials are added.

Recommended next implementation:

1. Add Twelve Data API-key config under `app.provider.twelvedata`.
2. Implement `MarketDataProvider` for Twelve Data prices and FX.
3. Keep Roundhill issuer data as the holdings and official NAV source.
4. Map provider records into `MarketDataIngestionRequest`.
5. Schedule the provider job for `02:00` and `16:30` Central Time.
