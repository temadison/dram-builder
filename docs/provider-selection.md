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

## Validated Symbol Map

Twelve Data `symbol_search` resolved the following symbols on June 28, 2026. The app's `dev` profile uses the provider `exchange` value below because that is the value returned by Twelve Data; the MIC is retained here for auditability.

| Holding | Symbol | Provider Exchange | MIC | Currency |
| --- | --- | --- | --- | --- |
| Roundhill Memory ETF | DRAM | NYSE | XASE | USD |
| Micron Technology | MU | NASDAQ | XNGS | USD |
| SanDisk | SNDK | NASDAQ | XNGS | USD |
| Western Digital | WDC | NASDAQ | XNGS | USD |
| Seagate Technology | STX | NASDAQ | XNGS | USD |
| SK hynix | 000660 | KRX | XKRX | KRW |
| Samsung Electronics | 005930 | KRX | XKRX | KRW |
| Kioxia Holdings | 285A | JPX | XJPX | JPY |
| Nanya Technology | 2408 | TWSE | XTAI | TWD |
| Winbond Electronics | 2344 | TWSE | XTAI | TWD |
| Macronix International | 2337 | TWSE | XTAI | TWD |
| Phison Electronics | 8299 | TWSE | XTAI | TWD |
| GigaDevice Semiconductor | 603986 | SSE | XSHG | CNY |

Roundhill lists DRAM's primary exchange as Cboe BZX, but Twelve Data `symbol_search` currently resolves the U.S. DRAM ETF row as `exchange=NYSE`, `mic_code=XASE`. Keep the provider query exchange aligned with Twelve Data, and set `output-exchange: BZX` so stored prices match the app's canonical freshness and snapshot-readiness key.

SK hynix is tracked in two forms: Korean ordinary shares as `KRX:000660` and the U.S. ADR as `NASDAQ:SKHY`. The dashboard parity component treats `10` SKHY ADRs as equivalent to one Korean ordinary share, so parity is calculated as `KRX close * KRW/USD / 10` and the ADR premium/discount is `SKHY / parity - 1`.

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
          exchange: NYSE
          name: Roundhill Memory ETF
          currency: USD
        kioxia:
          symbol: "285A"
          exchange: JPX
          name: Kioxia Holdings
          currency: JPY
```

Enable it only after the API key and close retrieval have been validated. If `enabled=true` without an API key, provider ingestion records a failed run with a clear configuration message.

The current adapter calls Twelve Data `time_series` with `interval=1day` and `outputsize=2` for each configured equity symbol. It first tries direct non-USD currency pairs such as `KRW/USD` for FX snapshots; if a direct pair is unavailable, it falls back to the inverse pair such as `USD/KRW` and stores the inverted USD conversion rate.

Provider ingestion can optionally include a DRAM snapshot request from `app.dram.snapshot`. The dev profile now contains the complete June 26, 2026 Roundhill-derived holdings set, but `app.dram.snapshot.enabled=false` by default. Keep it disabled until the Twelve Data close retrieval run succeeds for every configured price and FX input. Roundhill remains the issuer source for holdings, weights, and official NAV.

## Subscription Assumption

Plan for Twelve Data Pro or better. The required Asia-Pacific exchanges are not all Basic-plan markets.

## Validation Before Scheduled Use

Before using the provider in scheduled mode, confirm with a Twelve Data API key:

1. `time_series` returns two daily closes for each configured symbol in `src/main/resources/application-dev.yml`.
2. DRAM ETF closes are usable with the configured `DRAM` / `NYSE` pair, or the config is adjusted to the exchange alias Twelve Data accepts for the U.S. ETF.
3. Direct FX pairs `KRW/USD`, `JPY/USD`, `TWD/USD`, and `CNY/USD` are available for the chosen plan, or inverse pairs are available for adapter fallback.
4. Rate limits for the chosen plan can support the full run: 13 equity/ETF series plus four FX series, each with `outputsize=2`.

If any required holding is unavailable, keep Twelve Data for prices/FX where available and add a second provider only for the missing exchange rather than replacing the whole adapter.
