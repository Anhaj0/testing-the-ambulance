// Shared request wrapper for every module's API calls. Every endpoint in
// this app now replies with either a JSON body or no body at all (204) —
// including the dispatch endpoints, which used to return plain text — so
// one implementation covers all of them; module-specific fetch wrappers
// have been retired in favour of this one.
//
// On failure, the backend's GlobalExceptionHandler always replies with the
// same ErrorResponse shape: { errorCode, message, status, timestamp,
// details }, where `details` is a field -> message map for validation
// failures and null otherwise. We surface `message`, plus each field's
// detail when present, instead of a bare HTTP status.

async function readBody(res) {
  if (res.status === 204) return null;
  const text = await res.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return text; // not JSON (e.g. a proxy error page) — fall back to raw text
  }
}

function errorMessage(url, res, body) {
  if (body && typeof body === 'object') {
    if (body.message) {
      const details = body.details && typeof body.details === 'object'
        ? Object.entries(body.details).map(([field, msg]) => `${field}: ${msg}`).join('; ')
        : '';
      return details ? `${body.message} (${details})` : body.message;
    }
  } else if (typeof body === 'string' && body.trim()) {
    return body;
  }
  return `${url} -> HTTP ${res.status}`;
}

async function request(url, options) {
  const res = await fetch(url, options);
  const body = await readBody(res);
  if (!res.ok) {
    throw new Error(errorMessage(url, res, body));
  }
  return body;
}

export function getJson(url) {
  return request(url);
}
export function postJson(url, body) {
  return request(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
}
export function putJson(url, body) {
  return request(url, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
}
export function deleteJson(url) {
  return request(url, { method: 'DELETE' });
}
