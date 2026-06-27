import {
  getBridgeScore,
  getIngestionRuns,
  getLatestSnapshot,
  getMarketData,
  importMarketData,
  importMarketDataCsv,
  runScenario,
  saveFxRateSnapshot,
  saveOfficialNavSnapshot,
  savePriceSnapshot,
  saveSnapshot,
  saveSnapshotFromMarketData
} from './api.js';
import { sampleMarketData, sampleMarketDataCsv, sampleMarketDataSnapshot, sampleSnapshot } from './sampleData.js';
import {
  clearStatus,
  renderBridgeScore,
  renderEmpty,
  renderIngestionRuns,
  renderMarketData,
  renderScenario,
  renderSnapshot,
  showStatus
} from './view.js';

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

if (snapshotJson) {
  snapshotJson.value = JSON.stringify(sampleSnapshot, null, 2);
}
if (marketSnapshotJson) {
  marketSnapshotJson.value = JSON.stringify(sampleMarketDataSnapshot.holdings, null, 2);
}
if (marketDataCsv) {
  marketDataCsv.value = sampleMarketDataCsv;
}

bindClick('refresh-button', refresh);
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
    await importMarketDataCsv(marketDataCsv.value);
    marketSnapshotJson.value = JSON.stringify(sampleMarketDataSnapshot.holdings, null, 2);
    showStatus('CSV market data imported.', 'success');
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

async function refresh() {
  if (hasMarketData) {
    await refreshMarketData();
  }

  if (!hasDashboard) {
    clearStatus();
    return;
  }

  try {
    const [snapshot, bridgeScore] = await Promise.all([
      getLatestSnapshot(),
      getBridgeScore()
    ]);
    renderSnapshot(snapshot);
    renderBridgeScore(bridgeScore);
    setScenarioPurchasePrice(snapshot.purchasePrice);
    clearStatus();
  } catch (error) {
    if (error.status === 404) {
      renderEmpty();
      showStatus('No snapshot is available. Save the sample snapshot, generate one from market data, or start with local seed data.');
      return;
    }
    showStatus(error.message, 'error');
  }
}

async function refreshMarketData() {
  if (!hasMarketData) {
    return;
  }
  const [marketData, ingestionRuns] = await Promise.all([
    getMarketData(),
    getIngestionRuns()
  ]);
  renderMarketData(marketData);
  renderIngestionRuns(ingestionRuns);
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
