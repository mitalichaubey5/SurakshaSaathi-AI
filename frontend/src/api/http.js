/**
 * Thin fetch wrapper around the SurakshaSaathi backend.
 *
 * - All responses use the backend `ApiResponse` envelope
 *   { success, message, data, timestamp }.
 * - Errors are normalised into Error objects with a stable `code` so the UI
 *   can show the right friendly message:
 *     NETWORK     -> fetch threw (offline / server down)
 *     UNAUTHORIZED-> 401 (missing/expired token) - the stored token is cleared
 *     VALIDATION  -> 400 class (the user should fix their input)
 *     BACKEND     -> server answered but failed (e.g. 413, 5xx)
 * - No secrets or API keys live in the frontend. The default BASE_URL is the
 *   same origin (the Vite dev server proxies /api to the backend).
 * - The backend requires a JWT for protected routes, so the bearer token from
 *   the token store is attached as `Authorization: Bearer <token>` when present.
 */

import { getToken, clearToken } from './tokenStore';

export const ErrorCode = {
  NETWORK: 'NETWORK',
  UNAUTHORIZED: 'UNAUTHORIZED',
  VALIDATION: 'VALIDATION',
  BACKEND: 'BACKEND',
};

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

function httpError(code, status, message) {
  const err = new Error(message || 'request failed');
  err.code = code;
  err.status = status;
  return err;
}

/** Authorization header for the current session, or {} when signed out. */
function authHeaders() {
  const token = getToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function parseResponse(res) {
  let body = null;
  try {
    body = await res.json();
  } catch {
    body = null;
  }

  if (!res.ok) {
    const message = body && typeof body.message === 'string' ? body.message : null;
    if (res.status === 401) {
      // The token is missing/expired/invalid: drop it so the UI returns to sign-in.
      clearToken();
      throw httpError(ErrorCode.UNAUTHORIZED, res.status, message);
    }
    const code = res.status === 400 || res.status === 422 ? ErrorCode.VALIDATION : ErrorCode.BACKEND;
    throw httpError(code, res.status, message);
  }

  if (body && body.success === false) {
    throw httpError(ErrorCode.BACKEND, res.status, body.message || null);
  }
  return body;
}

async function guardNetwork(promise) {
  try {
    return await promise;
  } catch (err) {
    // fetch rejects with TypeError on network failure; rethrow our own typed error.
    if (err instanceof TypeError) {
      throw httpError(ErrorCode.NETWORK, 0, null);
    }
    throw err;
  }
}

export function getJson(path) {
  const headers = authHeaders();
  const url = BASE_URL + path;
  return guardNetwork(
    (Object.keys(headers).length ? fetch(url, { headers }) : fetch(url)).then(parseResponse),
  );
}

export function postJson(path, payload) {
  return guardNetwork(
    fetch(BASE_URL + path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(payload),
    }).then(parseResponse),
  );
}

export function postMultipart(path, formData, queryString = '') {
  const separator = queryString ? '?' + queryString : '';
  const headers = authHeaders();
  const init = {
    method: 'POST',
    body: formData, // browser sets the multipart Content-Type automatically
  };
  if (Object.keys(headers).length) {
    init.headers = headers;
  }
  return guardNetwork(fetch(BASE_URL + path + separator, init).then(parseResponse));
}
