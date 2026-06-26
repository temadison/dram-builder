import { getBridgeScore, getLatestSnapshot, runScenario, saveSnapshot } from './api.js';
import { sampleSnapshot } from './sampleData.js';
import { clearStatus, renderBridgeScore, renderEmpty, renderScenario, renderSnapshot, showStatus } from './view.js';

const snapshotJson = document.getElementById('snapshot-json');
const scenarioForm = document.getElementById('scenario-form');
const snapshotForm = document.getElementById('snapshot-form');

snapshotJson.value = JSON.stringify(sampleSnapshot, null, 2);

document.getElementById('refresh-button').addEventListener('click', refresh);
document.getElementById('sample-button').addEventListener('click', saveSampleSnapshot);
document.getElementById('reset-json-button').addEventListener('click', () => {
  snapshotJson.value = JSON.stringify(sampleSnapshot, null, 2);
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
      showStatus('No snapshot is available. Save the sample snapshot or start with local seed data.');
      return;
    }
    showStatus(error.message, 'error');
  }
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

function setScenarioPurchasePrice(value) {
  const input = scenarioForm.elements.namedItem('purchasePrice');
  if (input && value != null) {
    input.value = Number(value).toFixed(2);
  }
}

function numeric(value) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}
