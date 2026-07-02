# Roundhill Issuer Data Workflow

Roundhill is the source of truth for DRAM holdings, weights, market price, official NAV, and shares outstanding. Twelve Data should supply tradeable security closes and FX once the provider key is validated, but it should not replace issuer holdings or official NAV facts.

## Public Issuer Files

Roundhill's DRAM page loads issuer data from public CSV files under `https://www.roundhillinvestments.com/assets/data/`.

- Daily NAV: `FilepointRoundhill.40RU.RU_DailyNAV.csv`
- Holdings: `FilepointRoundhill.40RU.RU_Holdings_MMDDYYYY.csv`

The holdings filename uses a U.S. date stamp. The site attempts recent dates until it finds a populated file. When refreshing local data, use the latest holdings file whose row `Date` is not after the current calendar date.

## Current Local Baseline

The current local baseline in `data/ingest/dram-market-data-local.json` uses:

- Official NAV row from `FilepointRoundhill.40RU.RU_DailyNAV.csv`.
- DRAM market price from the same Daily NAV row's `Market Price`.
- DRAM holdings from the holdings CSV rows where `Account=DRAM`.
- Current observation date: `2026-06-26`.
- Prior observation date: `2026-06-25`.

The local JSON file is intentionally ignored by git. Treat it as an operational export, not source code.

## Transformation Rules

For app snapshot holdings, aggregate stock and total-return-swap exposure into one economic issuer row, because the snapshot validator rejects duplicate ticker/exchange holdings and the Roundhill UI also combines duplicate issuer exposure for DRAM top holdings.

Current aggregation keys:

| Issuer | Stock Row | Swap Rows |
| --- | --- | --- |
| Micron Technology | `MU` | `595112103 TRS 050427 NM`, `595112103 TRS 052427 GS` |
| SK hynix | `000660 KS` | `6450267 TRS 052427 GS` |
| Samsung Electronics | `005930 KS` | `6771720 TRS 052427 GS` |

Other equity rows are direct holdings:

| App Ticker | Issuer Row |
| --- | --- |
| `2337` | `2337 TT` |
| `2344` | `2344 TT` |
| `2408` | `2408 TT` |
| `285A` | `285A JP` |
| `603986` | `603986 C1` |
| `8299` | `8299 TT` |
| `SNDK` | `SNDK` |
| `STX` | `STX` |
| `WDC` | `WDC` |

Exclude cash, money market, and Treasury rows from app holdings unless the snapshot model is intentionally expanded to model collateral and cash drag.

## Implied FX

Until provider FX is validated, the local baseline derives issuer-implied FX from each foreign direct equity row:

```text
fx_to_usd = MarketValue / (Shares * Price)
```

Average rows by currency for the same observation date. Store the result as `roundhill-holdings-implied-fx`.

Provider FX should replace this once Twelve Data direct or inverse pairs are validated, because issuer-implied FX is an operational bridge rather than a market data feed.

## Refresh Checklist

The app now has a built-in manual refresh path: use `Issuer Refresh` on `/data.html` or call `POST /api/market-data/ingest/roundhill`. The endpoint applies these rules and records the attempt in ingestion history.

1. Download the latest non-future Daily NAV CSV and holdings CSV.
2. Confirm both files agree on the intended current date.
3. Download or retain the prior holdings file for prior closes/FX.
4. Rebuild `prices`, `fxRates`, `officialNavs`, and `snapshot.holdings`.
5. Run JSON validation.
6. Run a one-shot local ingestion with `SPRING_PROFILES_ACTIVE=local`.
7. Only use the file for MySQL or scheduled file ingestion after the one-shot run reports `snapshot=true`.
