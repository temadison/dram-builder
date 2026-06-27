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
- port: `3306`

The Spring `dev` profile is already configured for those values in `src/main/resources/application-dev.yml`. Flyway applies all migrations on startup.

## Prepare Ingestion Data

Create a JSON file using `docs/dev-market-data.example.json` as the shape. The example is not a current market data source; replace its prices, FX rates, official NAV, dates, and holdings with values from your provider.

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

The runner validates the file, stores price/FX/NAV snapshots through the same service paths used by the API, optionally creates a DRAM snapshot, and exits when `app.ingest.exit-after-run=true`.

## Run the App Against MySQL

After loading data:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun --args='--server.port=8081'
```

Then open:

```text
http://localhost:8081/
```

Useful checks:

```bash
curl http://localhost:8081/api/market-data
curl http://localhost:8081/api/dram/latest
curl http://localhost:8081/api/dram/bridge-score
```

## Remaining Provider Automation

The app now has a repeatable MySQL load path, but it does not yet fetch live provider APIs itself. To fully automate recent values, add a provider adapter that writes this ingestion file or calls `MarketDataService` directly.

Recommended next implementation:

1. Pick one provider for prices and FX.
2. Add API-key config under `app.provider`.
3. Add a provider client that resolves DRAM holdings, prices, FX, and official NAV.
4. Map provider records into `MarketDataIngestionRequest`.
5. Schedule or trigger the runner after market close.
