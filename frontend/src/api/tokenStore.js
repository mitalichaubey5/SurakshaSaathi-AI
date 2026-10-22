/**
 * Tiny, dependency-free JWT holder for the browser.
 *
 * - Persists the token in localStorage so a page refresh keeps the user signed in
 *   ("authentication persistence").
 * - Falls back to an in-memory value when localStorage is unavailable (private
 *   mode / SSR / tests) so the app never crashes.
 * - Stores ONLY the token. No personal data, no secrets beyond the bearer token.
 */

const STORAGE_KEY = 'surakshasaathi.authToken';

let memoryToken = null;

function storage() {
  try {
    if (typeof window !== 'undefined' && window.localStorage) {
      return window.localStorage;
    }
  } catch {
    // Access can throw (e.g. blocked storage); fall through to memory.
  }
  return null;
}

/** @returns {string|null} the current bearer token, or null when signed out. */
export function getToken() {
  const store = storage();
  if (store) {
    try {
      return store.getItem(STORAGE_KEY);
    } catch {
      return memoryToken;
    }
  }
  return memoryToken;
}

/** Stores the bearer token (pass null/'' to clear). */
export function setToken(token) {
  memoryToken = token || null;
  const store = storage();
  if (store) {
    try {
      if (token) {
        store.setItem(STORAGE_KEY, token);
      } else {
        store.removeItem(STORAGE_KEY);
      }
    } catch {
      // Ignore quota/security errors; the in-memory copy still works for this session.
    }
  }
}

/** Removes any stored token. */
export function clearToken() {
  setToken(null);
}
