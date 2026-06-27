export async function getLatestSnapshot() {
  return request('/api/dram/latest');
}

export async function getBridgeScore() {
  return request('/api/dram/bridge-score');
}

export async function runScenario(payload) {
  return request('/api/dram/scenario', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
}

export async function getMarketData() {
  return request('/api/market-data');
}

export async function getIngestionRuns() {
  return request('/api/market-data/ingestion-runs');
}

export async function getIngestionConfig() {
  return request('/api/market-data/ingestion-config');
}

export async function importMarketData(payload) {
  return request('/api/market-data/import', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
}

export async function importMarketDataCsv(csv) {
  return request('/api/market-data/import/csv', {
    method: 'POST',
    headers: { 'Content-Type': 'text/csv' },
    body: csv
  });
}

export async function runProviderIngestion(window = 'manual') {
  return request('/api/market-data/ingest/provider', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ window })
  });
}

export async function savePriceSnapshot(payload) {
  return request('/api/market-data/prices', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
}

export async function saveFxRateSnapshot(payload) {
  return request('/api/market-data/fx-rates', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
}

export async function saveOfficialNavSnapshot(payload) {
  return request('/api/market-data/official-navs', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
}

export async function saveSnapshot(payload) {
  return request('/api/dram/snapshot', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
}

export async function saveSnapshotFromMarketData(payload) {
  return request('/api/dram/snapshot/from-market-data', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
}

async function request(url, options = {}) {
  const response = await fetch(url, options);
  const contentType = response.headers.get('content-type') || '';
  const body = contentType.includes('application/json') ? await response.json() : await response.text();

  if (!response.ok) {
    const message = typeof body === 'object' && body.message ? body.message : `Request failed: ${response.status}`;
    const error = new Error(message);
    error.status = response.status;
    error.body = body;
    throw error;
  }

  return body;
}
