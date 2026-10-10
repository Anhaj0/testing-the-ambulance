import { getJson } from './client.js';

export function fetchNodes() {
  return getJson('/api/v1/network/graph/nodes');
}

export function fetchEdges() {
  return getJson('/api/v1/network/graph/edges');
}

export function fetchAmbulances() {
  return getJson('/api/v1/network/graph/ambulances');
}

export function fetchBlindSpots(thresholdMinutes) {
  return getJson(`/api/v1/network/blind-spots?thresholdMinutes=${thresholdMinutes}`);
}

// thresholds: array of numbers, e.g. [5, 10, 15, 20, 25, 30, 35, 40]
export function fetchCoverageCurve(thresholds) {
  const query = thresholds.join(',');
  return getJson(`/api/v1/network/coverage?thresholds=${query}`);
}
