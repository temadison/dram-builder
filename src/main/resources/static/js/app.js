import {
  getBridgeScore,
  getLatestSnapshot,
  getMarketData,
  importMarketData,
  runScenario,
  saveFxRateSnapshot,
  savePriceSnapshot,
  saveSnapshot,
  saveSnapshotFromMarketData
} from './api.js';
import { sampleMarketData, sampleMarketDataSnapshot, sampleSnapshot } from './sampleData.js';
import {
  clearStatus,
  renderBridgeScore,
  renderEmpty,
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
const marketSnapshotForm = document.getElementById('market-snapshot-form');

snapshotJson.value = JSON.stringify(sampleSnapshot, null, 2);
marketSnapshotJson.value = JSON.stringify(sampleMarketDataSnapshot.holdings, null, 2);

document.getElementById('refresh-button').addEventListener('click', refresh);
document.getElementById('sample-button').addEventListener('click', saveSampleSnapshot);
document.getElementById('load-market-sample-button').addEventListener('click', loadSampleMarketData);
document.getElementById('reset-json-button').addEventListener('click', () => {
  snapshotJson.value = JSON.stringify(sampleSnapshot, null, 2);
});
document.getElementById('reset-market-json-button').addEventListener('click', () => {
  marketSnapshotJson.value = JSON.stringify(sampleMarketDataSnapshot.holdings, null, 2);
});

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

refresh();

async function refresh() {
  await refreshMarketData();

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
  const marketData = await getMarketData();
  renderMarketData(marketData);
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
  const input = scenarioForm.elements.namedItem('purchasePrice');
  if (input && value != null) {
    input.value = Number(value).toFixed(2);
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
