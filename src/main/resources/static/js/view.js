import { dateTime, decimal, money, percent, signedClass } from './format.js';

export function showStatus(message, tone = 'info') {
  const band = document.getElementById('status-band');
  band.textContent = message;
  band.hidden = false;
  band.style.borderLeftColor = tone === 'error' ? 'var(--red)' : tone === 'success' ? 'var(--green)' : 'var(--amber)';
}

export function clearStatus() {
  const band = document.getElementById('status-band');
  band.hidden = true;
  band.textContent = '';
}

export function renderSnapshot(snapshot) {
  document.getElementById('snapshot-date').textContent = `Snapshot ${snapshot.asOfDate}`;
  document.getElementById('market-price').textContent = money(snapshot.marketPrice);
  document.getElementById('purchase-price').textContent = money(snapshot.purchasePrice);
  document.getElementById('synthetic-nav').textContent = money(snapshot.syntheticNav);
  document.getElementById('premium-discount').textContent = percent(snapshot.premiumDiscountPercent);
  document.getElementById('premium-discount').className = signedClass(snapshot.premiumDiscountPercent);
  document.getElementById('holding-count').textContent = `${snapshot.holdings.length} rows`;

  renderRows('holdings-table', snapshot.holdings, holding => `
    <tr>
      <td>${escapeHtml(holding.ticker)}</td>
      <td>${escapeHtml(holding.name)}</td>
      <td>${percent(Number(holding.weight) * 100)}</td>
      <td>${escapeHtml(holding.currency)}</td>
      <td class="${signedClass(holding.totalReturnPercent)}">${percent(holding.totalReturnPercent)}</td>
      <td class="${signedClass(holding.weightedContributionPercent)}">${percent(holding.weightedContributionPercent)}</td>
    </tr>
  `);

  renderAttribution(snapshot.attribution);
}

export function renderBridgeScore(score) {
  document.getElementById('bridge-score').textContent = decimal(score.score, 2);
  document.getElementById('rotation-signal').textContent = score.rotationSignal || '—';
  document.getElementById('recommendation').textContent = score.recommendation || '—';
  document.getElementById('score-updated').textContent = dateTime(score.createdAt);

  const components = score.components || {};
  const rows = [
    ['Target Exposure', components.targetExposureScore],
    ['Premium / Discount', components.premiumDiscountScore],
    ['Liquidity', components.liquidityScore],
    ['Tracking Confidence', components.trackingConfidenceScore],
    ['Timing Risk', components.timingRiskScore]
  ];

  document.getElementById('score-components').innerHTML = rows.map(([label, value]) => `
    <div class="component-row">
      <span>${label}</span>
      <strong>${decimal(value, 2)}</strong>
    </div>
  `).join('');
}

export function renderScenario(scenario) {
  document.getElementById('scenario-summary').textContent =
    `${percent(scenario.estimatedMovePercent)} / ${money(scenario.projectedMarketPrice)}`;

  renderRows('scenario-table', scenario.holdings, holding => `
    <tr>
      <td>${escapeHtml(holding.ticker)}</td>
      <td class="${signedClass(holding.securityMovePercent)}">${percent(holding.securityMovePercent)}</td>
      <td class="${signedClass(holding.fxMovePercent)}">${percent(holding.fxMovePercent)}</td>
      <td class="${signedClass(holding.weightedContributionPercent)}">${percent(holding.weightedContributionPercent)}</td>
    </tr>
  `);
}

export function renderMarketData(marketData) {
  const prices = marketData.latestPrices || [];
  const fxRates = marketData.latestFxRates || [];
  document.getElementById('market-data-summary').textContent =
    `${prices.length} prices / ${fxRates.length} FX`;

  const rows = [
    ...prices.map(price => ({
      type: 'Price',
      key: `${price.exchange}:${price.ticker}`,
      value: price.currency === 'USD' ? money(price.price) : decimal(price.price, 4),
      source: price.source
    })),
    ...fxRates.map(rate => ({
      type: 'FX',
      key: `${rate.baseCurrency}/${rate.quoteCurrency}`,
      value: decimal(rate.rate, 8),
      source: rate.source
    }))
  ];

  renderRows('market-data-table', rows.slice(0, 10), row => `
    <tr>
      <td>${escapeHtml(row.type)}</td>
      <td>${escapeHtml(row.key)}</td>
      <td>${escapeHtml(row.value)}</td>
      <td>${escapeHtml(row.source)}</td>
    </tr>
  `, 4);
}

export function renderAttribution(attribution) {
  const summary = document.getElementById('attribution-summary');
  if (!attribution || !attribution.hasPriorSnapshot) {
    summary.textContent = 'No prior snapshot';
    document.getElementById('attribution-table').innerHTML = emptyRow(4);
    return;
  }

  summary.textContent = `${percent(attribution.syntheticNavChangePercent)} NAV`;
  renderRows('attribution-table', attribution.topContributors, holding => `
    <tr>
      <td>${escapeHtml(holding.ticker)}</td>
      <td>${percent(holding.currentContributionPercent)}</td>
      <td>${percent(holding.priorContributionPercent)}</td>
      <td class="${signedClass(holding.contributionChangePercent)}">${percent(holding.contributionChangePercent)}</td>
    </tr>
  `);
}

export function renderEmpty() {
  document.getElementById('snapshot-date').textContent = 'No snapshot loaded';
  document.getElementById('market-price').textContent = '—';
  document.getElementById('purchase-price').textContent = '—';
  document.getElementById('synthetic-nav').textContent = '—';
  document.getElementById('premium-discount').textContent = '—';
  document.getElementById('bridge-score').textContent = '—';
  document.getElementById('rotation-signal').textContent = '—';
  document.getElementById('recommendation').textContent = '—';
  document.getElementById('holding-count').textContent = '0 rows';
  document.getElementById('holdings-table').innerHTML = emptyRow(6);
  document.getElementById('scenario-table').innerHTML = emptyRow(4);
  document.getElementById('attribution-table').innerHTML = emptyRow(4);
  document.getElementById('score-components').innerHTML = '';
}

function renderRows(id, rows, mapper, emptyColspan = 1) {
  document.getElementById(id).innerHTML = rows.length ? rows.map(mapper).join('') : emptyRow(emptyColspan);
}

function emptyRow(colspan) {
  return `<tr><td colspan="${colspan}">—</td></tr>`;
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, character => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  })[character]);
}
