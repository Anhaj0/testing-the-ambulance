import { getJson, postJson, putJson } from './client.js';

const triageBaseUrl = '/api/v1/triage/assessments';

export function evaluateTriage(assessment) {
  return postJson(triageBaseUrl, assessment);
}

export function fetchActiveQueue() {
  return getJson(`${triageBaseUrl}/queue`);
}

export function resolveAssessment(assessmentId) {
  return putJson(`${triageBaseUrl}/${assessmentId}/resolve`);
}
