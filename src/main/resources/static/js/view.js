import { dateOnly, dateTime, decimal, money, percent, signedClass } from './format.js?v=market-cap-start-20260716';

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

  const holdings = [...snapshot.holdings].sort((left, right) => Number(right.weight) - Number(left.weight));
  renderRows('holdings-table', holdings, holding => `
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

export function renderSkHynixComparison(comparison) {
  const parity = comparison?.latestParity;
  const dateElement = document.getElementById('skhynix-parity-date');
  if (!dateElement) {
    return;
  }

  dateElement.textContent = parity ? `Market cap chart / parity ${parity.date}` : 'Market cap chart';
  document.getElementById('skhynix-adr-price').textContent = money(parity?.adrPrice);
  document.getElementById('skhynix-local-equivalent').textContent = money(parity?.localEquivalentUsdPerAdr);
  const premiumElement = document.getElementById('skhynix-premium');
  premiumElement.textContent = percent(parity?.premiumDiscountPercent);
  premiumElement.className = signedClass(parity?.premiumDiscountPercent);

  renderPerformanceChart(comparison?.performance || []);
}

export function renderMarketData(marketData) {
  const prices = marketData.latestPrices || [];
  const fxRates = marketData.latestFxRates || [];
  const officialNavs = marketData.latestOfficialNavs || [];
  renderFreshness(marketData.freshness);
  renderSnapshotReadiness(marketData.snapshotReadiness, officialNavs[0]);

  const latestNav = officialNavs[0];
  const navSummary = latestNav ? ` / latest NAV ${latestNav.asOfDate}` : '';
  document.getElementById('market-data-summary').textContent =
    `${prices.length} prices / ${fxRates.length} FX / ${officialNavs.length} NAV${navSummary}`;

  const rows = [
    ...prices.map(price => ({
      type: 'Price',
      key: `${price.exchange}:${price.ticker}`,
      value: price.currency === 'USD' ? money(price.price) : decimal(price.price, 4),
      source: `${price.source} ${dateTime(price.observedAt)}`
    })),
    ...fxRates.map(rate => ({
      type: 'FX',
      key: `${rate.baseCurrency}/${rate.quoteCurrency}`,
      value: decimal(rate.rate, 8),
      source: `${rate.source} ${dateTime(rate.observedAt)}`
    })),
    ...officialNavs.map(nav => ({
      type: 'Official NAV',
      key: `${nav.ticker} ${nav.asOfDate}`,
      value: nav.currency === 'USD' ? money(nav.nav) : decimal(nav.nav, 4),
      source: `${nav.source} ${dateTime(nav.observedAt)}`
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

export function renderIngestionRuns(runs) {
  const recentRuns = runs || [];
  const summary = document.getElementById('ingestion-run-summary');
  if (summary) {
    const latest = recentRuns[0];
    summary.textContent = latest ? `${latest.status} ${dateTime(latest.completedAt || latest.startedAt)}` : 'No runs';
  }

  renderRows('ingestion-run-table', recentRuns, run => `
    <tr>
      <td class="${runStatusClass(run.status)}">${escapeHtml(run.status)}</td>
      <td>${escapeHtml(run.source)}</td>
      <td>${dateTime(run.completedAt || run.startedAt)}</td>
      <td>${run.pricesImported} / ${run.fxRatesImported} / ${run.officialNavsImported}</td>
      <td>${run.snapshotCreated ? 'Yes' : 'No'}</td>
      <td>${escapeHtml(run.message || run.requestedFile || '—')}</td>
    </tr>
  `, 6);
}

export function renderIngestionConfig(config) {
  if (!config) {
    return;
  }

  const rows = [
    ['Runner', config.runnerEnabled ? 'Enabled' : 'Disabled'],
    ['Schedule', config.scheduleEnabled ? 'Enabled' : 'Disabled'],
    ['Mode', config.scheduleMode || 'file'],
    ['Zone', config.scheduleZone || '—'],
    ['Morning', config.morningCron || '—'],
    ['Evening', config.eveningCron || '—'],
    ['File', config.ingestionFile || '—'],
    ['Providers', String(config.providerCount ?? 0)],
    ['Freshness', `${config.freshnessMarketZone || '—'} after ${config.freshnessExpectedAfterLocalTime || '—'} / calendars ${config.freshnessExchangeCalendars || '—'} / holidays ${config.freshnessMarketHolidays || '—'} / ${config.freshnessRequiredPrices || '—'}`]
  ];

  renderRows('ingestion-config-table', rows, ([label, value]) => `
    <tr>
      <td>${escapeHtml(label)}</td>
      <td>${escapeHtml(value)}</td>
    </tr>
  `, 2);
}

function renderFreshness(freshness) {
  const status = freshness?.status || 'UNKNOWN';
  const statusElement = document.getElementById('freshness-status');
  statusElement.textContent = status;
  statusElement.className = freshnessClass(status);

  document.getElementById('freshness-checked').textContent = `Checked ${dateTime(freshness?.checkedAt)}`;
  document.getElementById('freshness-threshold').textContent =
    freshness?.expectedAsOfDate
      ? `Expected ${dateOnly(freshness.expectedAsOfDate)} after ${freshness.expectedAfterLocalTime || '—'} ${freshness.marketZone || ''}`.trim()
      : 'Expected market date —';

  const rows = freshness?.requiredPrices || [];
  renderRows('freshness-table', rows, row => `
    <tr>
      <td>${escapeHtml(row.exchange)}:${escapeHtml(row.ticker)}</td>
      <td>${dateOnly(row.expectedAsOfDate)}</td>
      <td>${dateTime(row.latestObservedAt)}</td>
      <td class="${freshnessClass(rowStatus(row))}">${rowStatus(row)}</td>
    </tr>
  `, 4);
}

function renderSnapshotReadiness(readiness, latestOfficialNav) {
  const status = readiness?.status || 'UNKNOWN';
  const statusElement = document.getElementById('snapshot-readiness-status');
  if (!statusElement) {
    return;
  }

  statusElement.textContent = status;
  statusElement.className = readinessClass(status);
  document.getElementById('snapshot-readiness-date').textContent = `As of ${readiness?.asOfDate || '—'}`;
  document.getElementById('snapshot-readiness-nav').textContent = latestOfficialNav
    ? `Official NAV ${money(latestOfficialNav.nav)} ${latestOfficialNav.asOfDate}`
    : 'Official NAV —';

  const issues = readiness?.issues || [];
  const issueElement = document.getElementById('snapshot-readiness-issues');
  issueElement.innerHTML = issues.length ? issues.slice(0, 8).map(issue => `
    <div class="readiness-issue">
      <strong>${escapeHtml(issue.category)}</strong>
      <span>${escapeHtml(issue.key)} · ${escapeHtml(issue.message)}</span>
    </div>
  `).join('') : '<div class="readiness-issue ready">All configured snapshot inputs are present.</div>';
}

function rowStatus(row) {
  if (row.missing) {
    return 'MISSING';
  }
  if (row.stale) {
    return 'STALE';
  }
  return 'FRESH';
}

function readinessClass(status) {
  if (status === 'READY') {
    return 'positive';
  }
  if (status === 'BLOCKED') {
    return 'negative';
  }
  return 'neutral';
}

function freshnessClass(status) {
  if (status === 'FRESH') {
    return 'positive';
  }
  if (status === 'STALE') {
    return 'negative';
  }
  return 'neutral';
}

function runStatusClass(status) {
  if (status === 'SUCCESS') {
    return 'positive';
  }
  if (status === 'FAILED') {
    return 'negative';
  }
  return 'neutral';
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
  renderSkHynixComparison(null);
}

function renderPerformanceChart(points) {
  const svg = document.getElementById('skhynix-performance-chart');
  const legend = document.getElementById('skhynix-performance-legend');
  if (!svg || !legend) {
    return;
  }

  const series = groupBySymbol(points);
  const symbols = Object.keys(series);
  if (symbols.length === 0) {
    svg.innerHTML = '<text x="360" y="132" text-anchor="middle" class="chart-empty">No market cap history</text>';
    legend.innerHTML = '';
    return;
  }

  const width = 720;
  const height = 260;
  const margin = { top: 18, right: 22, bottom: 34, left: 58 };
  const values = points.map(point => billions(point.marketCapUsd)).filter(Number.isFinite);
  const rawMinValue = Math.min(...values);
  const rawMaxValue = Math.max(...values);
  const padding = Math.max((rawMaxValue - rawMinValue) * 0.08, 1);
  const minValue = Math.max(0, rawMinValue - padding);
  const maxValue = rawMaxValue + padding;
  const dates = [...new Set(points.map(point => point.date))].sort();
  const xForDate = date => {
    const index = dates.indexOf(date);
    if (dates.length <= 1) {
      return margin.left;
    }
    return margin.left + (index / (dates.length - 1)) * (width - margin.left - margin.right);
  };
  const yForValue = value => {
    const range = maxValue - minValue || 1;
    return margin.top + ((maxValue - value) / range) * (height - margin.top - margin.bottom);
  };
  const palette = {
    SKHY: 'var(--teal)',
    '000660': 'var(--blue)',
    MU: 'var(--green)'
  };

  const gridValues = [minValue, (minValue + maxValue) / 2, maxValue];
  const grid = gridValues.map(value => `
    <g>
      <line x1="${margin.left}" y1="${yForValue(value)}" x2="${width - margin.right}" y2="${yForValue(value)}" class="chart-grid-line"></line>
      <text x="${margin.left - 8}" y="${yForValue(value) + 4}" text-anchor="end" class="chart-axis-label">$${decimal(value, 0)}B</text>
    </g>
  `).join('');
  const axisTitle = '<text x="14" y="132" text-anchor="middle" class="chart-axis-label" transform="rotate(-90 14 132)">Market cap ($B)</text>';

  const paths = symbols.map(symbol => {
    const path = series[symbol]
      .sort((left, right) => left.date.localeCompare(right.date))
      .map((point, index) => `${index === 0 ? 'M' : 'L'} ${xForDate(point.date).toFixed(2)} ${yForValue(billions(point.marketCapUsd)).toFixed(2)}`)
      .join(' ');
    return `<path d="${path}" class="chart-line" style="stroke: ${palette[symbol] || 'var(--muted)'}"></path>`;
  }).join('');

  const labels = dates.length ? `
    <text x="${margin.left}" y="${height - 10}" class="chart-axis-label">${escapeHtml(dates[0])}</text>
    <text x="${width - margin.right}" y="${height - 10}" text-anchor="end" class="chart-axis-label">${escapeHtml(dates[dates.length - 1])}</text>
  ` : '';

  svg.innerHTML = `${grid}${axisTitle}${paths}${labels}`;
  legend.innerHTML = symbols.map(symbol => {
    const sorted = [...series[symbol]].sort((left, right) => left.date.localeCompare(right.date));
    const latest = sorted[sorted.length - 1];
    const label = latest?.label || symbol;
    const marketCap = latest?.marketCapUsd == null ? '—' : `$${decimal(billions(latest.marketCapUsd), 1)}B`;
    return `<span><i style="background: ${palette[symbol] || 'var(--muted)'}"></i>${escapeHtml(label)} ${escapeHtml(marketCap)}</span>`;
  }).join('');
}

function groupBySymbol(points) {
  return points.reduce((groups, point) => {
    if (!groups[point.symbol]) {
      groups[point.symbol] = [];
    }
    groups[point.symbol].push(point);
    return groups;
  }, {});
}

function billions(value) {
  return Number(value) / 1000000000;
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
