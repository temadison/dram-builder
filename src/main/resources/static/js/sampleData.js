export const sampleSnapshot = {
  asOfDate: '2026-06-26',
  marketPrice: 81.50,
  purchasePrice: 76.31,
  holdings: [
    {
      ticker: '000660',
      name: 'SK hynix',
      exchange: 'KRX',
      currency: 'KRW',
      weight: 0.26,
      currentPrice: 114000,
      priorPrice: 110000,
      currentFxToUsd: 0.00081,
      priorFxToUsd: 0.00080
    },
    {
      ticker: 'MU',
      name: 'Micron Technology',
      exchange: 'NASDAQ',
      currency: 'USD',
      weight: 0.19,
      currentPrice: 108,
      priorPrice: 105,
      currentFxToUsd: 1,
      priorFxToUsd: 1
    },
    {
      ticker: '005930',
      name: 'Samsung Electronics',
      exchange: 'KRX',
      currency: 'KRW',
      weight: 0.15,
      currentPrice: 79000,
      priorPrice: 77600,
      currentFxToUsd: 0.00081,
      priorFxToUsd: 0.00082
    },
    {
      ticker: 'ASML',
      name: 'ASML Holding',
      exchange: 'NASDAQ',
      currency: 'USD',
      weight: 0.05,
      currentPrice: 1015,
      priorPrice: 1020,
      currentFxToUsd: 1,
      priorFxToUsd: 1
    }
  ]
};

export const sampleMarketDataSnapshot = {
  asOfDate: '2026-06-26',
  purchasePrice: 76.31,
  holdings: sampleSnapshot.holdings.map(({ ticker, name, exchange, currency, weight }) => ({
    ticker,
    name,
    exchange,
    currency,
    weight
  }))
};

export const sampleMarketData = {
  prices: [
    price('DRAM', 'Roundhill Memory ETF', 'NYSEARCA', 'USD', 81.50, '2026-06-26T20:00:00Z'),
    price('000660', 'SK hynix', 'KRX', 'KRW', 110000, '2026-06-25T20:00:00Z'),
    price('000660', 'SK hynix', 'KRX', 'KRW', 114000, '2026-06-26T20:00:00Z'),
    price('MU', 'Micron Technology', 'NASDAQ', 'USD', 105, '2026-06-25T20:00:00Z'),
    price('MU', 'Micron Technology', 'NASDAQ', 'USD', 108, '2026-06-26T20:00:00Z'),
    price('005930', 'Samsung Electronics', 'KRX', 'KRW', 77600, '2026-06-25T20:00:00Z'),
    price('005930', 'Samsung Electronics', 'KRX', 'KRW', 79000, '2026-06-26T20:00:00Z'),
    price('ASML', 'ASML Holding', 'NASDAQ', 'USD', 1020, '2026-06-25T20:00:00Z'),
    price('ASML', 'ASML Holding', 'NASDAQ', 'USD', 1015, '2026-06-26T20:00:00Z')
  ],
  fxRates: [
    fxRate('KRW', 'USD', 0.00080, '2026-06-25T20:00:00Z'),
    fxRate('KRW', 'USD', 0.00081, '2026-06-26T20:00:00Z')
  ]
};

function price(ticker, name, exchange, currency, priceValue, observedAt) {
  return {
    ticker,
    name,
    exchange,
    currency,
    price: priceValue,
    source: 'ui-sample',
    observedAt
  };
}

function fxRate(baseCurrency, quoteCurrency, rate, observedAt) {
  return {
    baseCurrency,
    quoteCurrency,
    rate,
    source: 'ui-sample',
    observedAt
  };
}
