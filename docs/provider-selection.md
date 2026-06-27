# Market Data Provider Selection

## Decision

Use Twelve Data as the first automated price and FX provider.

Use Roundhill as the issuer source for DRAM holdings, weights, and official NAV when available. Holdings and NAV are fund facts, so the issuer should remain the source of truth rather than a generic market data vendor.

## Why Twelve Data

The DRAM workflow needs U.S. ETF/equity prices, Korea/Japan/Taiwan equity prices, and FX rates. Twelve Data is the best initial fit because its published exchange coverage includes:

- United States market data on the Basic plan.
- Korea Stock Exchange (`XKRX`) and KOSDAQ (`XKOS`) end-of-day data on Pro.
- Tokyo Stock Exchange (`XJPX`) coverage.
- Taiwan Stock Exchange (`XTAI`) and Taipei Exchange (`ROCO`) end-of-day data on Pro.
- Forex data, including USD, KRW, JPY, and other major currencies.

This matches the current DRAM universe better than a U.S.-focused provider and keeps the adapter surface simple: one provider for prices and FX, with issuer data layered in separately.

## Initial Symbol Map

Use this as the starting point for the provider adapter. Confirm exact symbols through Twelve Data symbol search before hard-coding.

| Holding | Local Symbol | Exchange | Currency |
| --- | --- | --- | --- |
| Roundhill Memory ETF | DRAM | United States / BATS or Cboe BZX | USD |
| Micron Technology | MU | United States / NASDAQ | USD |
| SanDisk | SNDK | United States / NASDAQ | USD |
| Western Digital | WDC | United States / NASDAQ | USD |
| Seagate Technology | STX | United States / NASDAQ | USD |
| SK hynix | 000660 | XKRX | KRW |
| Samsung Electronics | 005930 | XKRX | KRW |
| Kioxia | TBD | XJPX | JPY |
| Nanya Technology | TBD | XTAI | TWD |
| Winbond Electronics | TBD | XTAI or ROCO | TWD |

## Provider Scope

The first adapter should fetch:

- Latest or most recent available end-of-day equity close for each configured holding.
- Prior comparable close for return calculation.
- FX rates from each holding currency to USD.
- Optional intraday/recent data later, only after EOD ingestion is stable.

The adapter should not fetch or infer holdings. Holdings should come from Roundhill issuer data or a local issuer export until a reliable official machine-readable feed is added.

## App Configuration

The application now has a disabled-by-default Twelve Data provider scaffold:

```yaml
app:
  provider:
    twelvedata:
      enabled: false
      api-key: ${TWELVE_DATA_API_KEY:}
      base-url: https://api.twelvedata.com
      symbols:
        dram:
          symbol: DRAM
          exchange: BATS
          name: Roundhill Memory ETF
          currency: USD
```

Enable it only after the API key and symbol map have been validated. If `enabled=true` without an API key, provider ingestion records a failed run with a clear configuration message.

The current adapter calls Twelve Data `time_series` with `interval=1day` and `outputsize=2` for each configured equity symbol. It first tries direct non-USD currency pairs such as `KRW/USD` for FX snapshots; if a direct pair is unavailable, it falls back to the inverse pair such as `USD/KRW` and stores the inverted USD conversion rate.

Provider ingestion can optionally include a DRAM snapshot request from `app.dram.snapshot`. Keep `app.dram.snapshot.enabled=false` until the purchase price and complete Roundhill holdings file are validated. The dev profile includes a partial U.S.-listed starter holdings set only; Roundhill remains the issuer source for complete holdings, weights, and official NAV.

## Subscription Assumption

Plan for Twelve Data Pro or better. The required Asia-Pacific exchanges are not all Basic-plan markets.

## Validation Before Implementation

Before using the provider in scheduled mode, confirm with a Twelve Data API key:

1. The exact symbol/exchange pair for each DRAM holding.
2. Whether DRAM ETF quotes resolve as `DRAM` on the expected U.S. exchange.
3. Whether Kioxia, Nanya, and Winbond resolve on their primary local exchanges.
4. Whether direct FX pairs like `KRW/USD`, `JPY/USD`, and `TWD/USD` are available for the chosen plan; the adapter can fall back to inverted `USD/KRW`, `USD/JPY`, and `USD/TWD`.
5. Rate limits for the chosen plan.

If any required holding is unavailable, keep Twelve Data for prices/FX where available and add a second provider only for the missing exchange rather than replacing the whole adapter.
