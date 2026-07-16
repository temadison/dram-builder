import {
  getBridgeScore,
  getIngestionConfig,
  getIngestionRuns,
  getLatestSnapshot,
  getMarketData,
  getSkHynixComparison,
  importMarketData,
  importMarketDataCsv,
  runScenario,
  runFileIngestion,
  runProviderIngestion,
  runRoundhillIngestion,
  saveFxRateSnapshot,
  saveOfficialNavSnapshot,
  savePriceSnapshot,
  saveSnapshot,
  saveSnapshotFromMarketData
} from './api.js?v=market-cap-start-20260716';
import { sampleMarketData, sampleMarketDataCsv, sampleMarketDataSnapshot, sampleSnapshot } from './sampleData.js?v=market-cap-start-20260716';
import {
  clearStatus,
  renderBridgeScore,
  renderEmpty,
  renderIngestionConfig,
  renderIngestionRuns,
  renderMarketData,
  renderScenario,
  renderSkHynixComparison,
  renderSnapshot,
  showStatus
} from './view.js?v=market-cap-start-20260716';

const snapshotJson = document.getElementById('snapshot-json');
const marketSnapshotJson = document.getElementById('market-snapshot-json');
const scenarioForm = document.getElementById('scenario-form');
const snapshotForm = document.getElementById('snapshot-form');
const priceForm = document.getElementById('price-form');
const fxForm = document.getElementById('fx-form');
const officialNavForm = document.getElementById('official-nav-form');
const csvImportForm = document.getElementById('csv-import-form');
const marketDataCsv = document.getElementById('market-data-csv');
const marketSnapshotForm = document.getElementById('market-snapshot-form');
const hasDashboard = Boolean(document.getElementById('market-price'));
const hasMarketData = Boolean(document.getElementById('market-data-summary'));
let dashboardAutoLoadAttempted = false;

if (snapshotJson) {
  snapshotJson.value = JSON.stringify(sampleSnapshot, null, 2);
}
if (marketSnapshotJson) {
  marketSnapshotJson.value = JSON.stringify(sampleMarketDataSnapshot.holdings, null, 2);
}
if (marketDataCsv) {
  marketDataCsv.value = sampleMarketDataCsv;
}

bindClick('refresh-button', refreshLatestData);
bindClick('sample-button', saveSampleSnapshot);
bindClick('load-market-sample-button', loadSampleMarketData);
bindClick('reset-market-csv-button', () => {
  marketDataCsv.value = sampleMarketDataCsv;
});
bindClick('reset-json-button', () => {
  snapshotJson.value = JSON.stringify(sampleSnapshot, null, 2);
});
bindClick('reset-market-json-button', () => {
  marketSnapshotJson.value = JSON.stringify(sampleMarketDataSnapshot.holdings, null, 2);
});
bindClick('run-file-ingestion-button', runFileIngestionFromUi);
bindClick('run-provider-ingestion-button', runProviderIngestionFromUi);
bindClick('refresh-roundhill-ingestion-button', runRoundhillIngestionFromUi);

if (snapshotForm) {
  snapshotForm.addEventListener('submit', async event => {
  event.preventDefault();
  try {
    const payload = JSON.parse(snapshotJson.value);
    await saveSnapshot(payload);
    showStatus('Snapshot saved.', 'success');
    await refresh();
  } catch (error) {
    showStatus(error.message, 'error');
  }
  });
}

if (priceForm) {
  priceForm.addEventListener('submit', async event => {
  event.preventDefault();
  const form = new FormData(priceForm);
  const payload = {
    ticker: text(form.get('ticker')),
    name: text(form.get('name')),
    exchange: text(form.get('exchange')),
    currency: text(form.get('currency')),
    price: numeric(form.get('price')),
    source: text(form.get('source'))
  };

  try {
    await savePriceSnapshot(payload);
    showStatus('Price snapshot saved.', 'success');
    await refreshMarketData();
  } catch (error) {
    showStatus(error.message, 'error');
  }
  });
}

if (fxForm) {
  fxForm.addEventListener('submit', async event => {
  event.preventDefault();
  const form = new FormData(fxForm);
  const payload = {
    baseCurrency: text(form.get('baseCurrency')),
    quoteCurrency: text(form.get('quoteCurrency')),
    rate: numeric(form.get('rate')),
    source: text(form.get('source'))
  };

  try {
    await saveFxRateSnapshot(payload);
    showStatus('FX rate snapshot saved.', 'success');
    await refreshMarketData();
  } catch (error) {
    showStatus(error.message, 'error');
  }
  });
}

if (officialNavForm) {
  officialNavForm.addEventListener('submit', async event => {
  event.preventDefault();
  const form = new FormData(officialNavForm);
  const payload = {
    ticker: text(form.get('ticker')),
    name: text(form.get('name')),
    nav: numeric(form.get('nav')),
    currency: text(form.get('currency')),
    source: text(form.get('source')),
    asOfDate: text(form.get('asOfDate'))
  };

  try {
    await saveOfficialNavSnapshot(payload);
    showStatus('Official NAV snapshot saved.', 'success');
    await refreshMarketData();
  } catch (error) {
    showStatus(error.message, 'error');
  }
  });
}

if (csvImportForm) {
  csvImportForm.addEventListener('submit', async event => {
  event.preventDefault();

  try {
    const result = await importMarketDataCsv(marketDataCsv.value);
    marketSnapshotJson.value = JSON.stringify(sampleMarketDataSnapshot.holdings, null, 2);
    showStatus(importSummary(result), 'success');
    await refreshMarketData();
  } catch (error) {
    showStatus(error.message, 'error');
  }
  });
}

if (marketSnapshotForm) {
  marketSnapshotForm.addEventListener('submit', async event => {
  event.preventDefault();

  try {
    const form = new FormData(marketSnapshotForm);
    const payload = {
      asOfDate: text(form.get('asOfDate')) || null,
      marketPrice: optionalNumeric(form.get('marketPrice')),
      purchasePrice: numeric(form.get('purchasePrice')),
      holdings: JSON.parse(marketSnapshotJson.value)
    };
    await saveSnapshotFromMarketData(payload);
    showStatus('Snapshot generated from stored market data.', 'success');
    await refresh();
  } catch (error) {
    showStatus(error.message, 'error');
  }
  });
}

if (scenarioForm) {
  scenarioForm.addEventListener('submit', async event => {
  event.preventDefault();
  const form = new FormData(scenarioForm);
  const payload = {
    name: 'UI scenario',
    purchasePrice: numeric(form.get('purchasePrice')),
    securityMovesPercent: {
      '000660': numeric(form.get('hynix')),
      MU: numeric(form.get('micron')),
      '005930': numeric(form.get('samsung'))
    },
    fxMovesPercent: {
      KRW: numeric(form.get('krw'))
    }
  };

  try {
    const scenario = await runScenario(payload);
    renderScenario(scenario);
    clearStatus();
  } catch (error) {
    showStatus(error.message, 'error');
  }
  });
}

refresh();

async function refreshLatestData() {
  if (hasMarketData) {
    await runRoundhillIngestionFromUi();
    return;
  }
  await reloadView();
}

async function reloadView() {
  const button = document.getElementById('refresh-button');
  try {
    setBusy(button, true, 'Reloading...');
    showStatus('Reloading stored data...');
    const result = await refresh();
    if (result.loaded) {
      showStatus(reloadSummary(result.marketData), result.marketData?.freshness?.status === 'FRESH' ? 'success' : 'info');
    }
  } finally {
    setBusy(button, false);
  }
}

async function refresh() {
  let marketData = null;
  if (hasMarketData) {
    try {
      marketData = await refreshMarketData();
    } catch (error) {
      showStatus(`Unable to load market data: ${error.message}`, 'error');
      return { loaded: false, marketData: null };
    }
  }

  if (!hasDashboard) {
    clearStatus();
    return { loaded: true, marketData };
  }

  try {
    const [snapshot, bridgeScore, skHynixComparison] = await Promise.all([
      getLatestSnapshot(),
      getBridgeScore(),
      getSkHynixComparison()
    ]);
    renderSnapshot(snapshot);
    renderBridgeScore(bridgeScore);
    renderSkHynixComparison(skHynixComparison);
    setScenarioPurchasePrice(snapshot.purchasePrice);
    clearStatus();
    return { loaded: true, marketData };
  } catch (error) {
    if (error.status === 404) {
      if (hasDashboard && !dashboardAutoLoadAttempted) {
        dashboardAutoLoadAttempted = true;
        await autoLoadDashboard();
        return { loaded: true, marketData };
      }
      renderEmpty();
      showStatus('No snapshot is available. Save the sample snapshot, generate one from market data, or start with local seed data.');
      return { loaded: false, marketData };
    }
    showStatus(error.message, 'error');
    return { loaded: false, marketData };
  }
}

async function autoLoadDashboard() {
  const button = document.getElementById('refresh-button');
  try {
    setBusy(button, true, '...');
    renderEmpty();
    showStatus('No snapshot is available. Loading latest issuer data...');
    const runs = await runRoundhillIngestion();
    const [snapshot, bridgeScore, skHynixComparison] = await Promise.all([
      getLatestSnapshot(),
      getBridgeScore(),
      getSkHynixComparison()
    ]);
    renderSnapshot(snapshot);
    renderBridgeScore(bridgeScore);
    renderSkHynixComparison(skHynixComparison);
    setScenarioPurchasePrice(snapshot.purchasePrice);
    showStatus(ingestionSummary(runs?.[0], 'Latest issuer data loaded.'), 'success');
  } catch (error) {
    renderEmpty();
    showStatus(`Unable to auto-load dashboard data: ${error.message}`, 'error');
  } finally {
    setBusy(button, false);
  }
}

async function refreshMarketData() {
  if (!hasMarketData) {
    return null;
  }
  const [marketData, ingestionRuns, ingestionConfig] = await Promise.all([
    getMarketData(),
    getIngestionRuns(),
    getIngestionConfig()
  ]);
  renderMarketData(marketData);
  renderIngestionRuns(ingestionRuns);
  renderIngestionConfig(ingestionConfig);
  return marketData;
}

async function saveSampleSnapshot() {
  try {
    await saveSnapshot(sampleSnapshot);
    showStatus('Sample snapshot loaded.', 'success');
    await refresh();
  } catch (error) {
    showStatus(error.message, 'error');
  }
}

async function loadSampleMarketData() {
  try {
    await importMarketData(sampleMarketData);
    marketSnapshotJson.value = JSON.stringify(sampleMarketDataSnapshot.holdings, null, 2);
    showStatus('Sample market data loaded.', 'success');
    await refreshMarketData();
  } catch (error) {
    showStatus(error.message, 'error');
  }
}

async function runProviderIngestionFromUi() {
  const button = document.getElementById('run-provider-ingestion-button');
  try {
    setBusy(button, true, 'Running...');
    showStatus('Running provider ingestion…');
    const runs = await runProviderIngestion();
    showStatus(ingestionSummary(runs?.[0], 'Provider ingestion completed.'), 'success');
    await refreshMarketData();
  } catch (error) {
    showStatus(`Provider ingestion failed: ${error.message}`, 'error');
  } finally {
    setBusy(button, false);
  }
}

async function runFileIngestionFromUi() {
  const button = document.getElementById('run-file-ingestion-button');
  try {
    setBusy(button, true, 'Importing...');
    showStatus('Importing local file…');
    const runs = await runFileIngestion();
    showStatus(ingestionSummary(runs?.[0], 'Local file imported.'), 'success');
    await refreshMarketData();
  } catch (error) {
    showStatus(`Local file import failed: ${error.message}`, 'error');
  } finally {
    setBusy(button, false);
  }
}

async function runRoundhillIngestionFromUi() {
  const buttons = [
    document.getElementById('refresh-button'),
    document.getElementById('refresh-roundhill-ingestion-button')
  ].filter(Boolean);
  try {
    setBusyAll(buttons, true, 'Refreshing...');
    showStatus('Refreshing latest Roundhill issuer data…');
    const runs = await runRoundhillIngestion();
    showStatus(ingestionSummary(runs?.[0], 'Latest Roundhill issuer data refreshed.'), 'success');
    await refreshMarketData();
  } catch (error) {
    showStatus(`Roundhill refresh failed: ${error.message}`, 'error');
  } finally {
    setBusyAll(buttons, false);
  }
}

function setScenarioPurchasePrice(value) {
  if (!scenarioForm) {
    return;
  }
  const input = scenarioForm.elements.namedItem('purchasePrice');
  if (input && value != null) {
    input.value = Number(value).toFixed(2);
  }
}

function bindClick(id, handler) {
  const element = document.getElementById(id);
  if (element) {
    element.addEventListener('click', handler);
  }
}

function importSummary(result) {
  const prices = result?.pricesImported ?? 0;
  const fxRates = result?.fxRatesImported ?? 0;
  const officialNavs = result?.officialNavsImported ?? 0;
  const snapshot = result?.snapshotCreated ? ' Snapshot created.' : '';
  return `CSV imported: ${prices} prices / ${fxRates} FX / ${officialNavs} NAV.${snapshot}`;
}

function ingestionSummary(run, fallback) {
  if (!run) {
    return fallback;
  }
  const snapshot = run.snapshotCreated ? 'snapshot created' : 'no snapshot';
  return `${fallback} ${run.pricesImported} prices / ${run.fxRatesImported} FX / ${run.officialNavsImported} NAV, ${snapshot}.`;
}

function reloadSummary(marketData) {
  const status = marketData?.freshness?.status;
  if (status === 'FRESH') {
    return 'Stored data reloaded. Required prices are fresh.';
  }
  if (status === 'STALE' || status === 'MISSING') {
    return `Stored data reloaded. Required prices are ${status.toLowerCase()}; run Issuer Refresh or Provider Prices to update source data.`;
  }
  return 'Stored data reloaded.';
}

function setBusy(button, busy, label) {
  if (!button) {
    return;
  }
  if (busy) {
    button.dataset.idleText = button.textContent;
    button.dataset.idleHtml = button.innerHTML;
    button.textContent = label;
    button.disabled = true;
    return;
  }
  if (button.dataset.idleHtml) {
    button.innerHTML = button.dataset.idleHtml;
  } else {
    button.textContent = button.dataset.idleText || button.textContent;
  }
  button.disabled = false;
}

function setBusyAll(buttons, busy, label) {
  buttons.forEach(button => setBusy(button, busy, label));
}

function optionalNumeric(value) {
  const clean = text(value);
  return clean === '' ? null : numeric(clean);
}

function numeric(value) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function text(value) {
  return String(value ?? '').trim();
}
