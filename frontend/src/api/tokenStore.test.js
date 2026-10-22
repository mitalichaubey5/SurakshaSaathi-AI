import { beforeEach, describe, expect, it } from 'vitest';
import { getToken, setToken, clearToken } from './tokenStore';

describe('tokenStore', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('starts signed out', () => {
    expect(getToken()).toBeNull();
  });

  it('persists and reads back a token', () => {
    setToken('header.payload.signature');
    expect(getToken()).toBe('header.payload.signature');
    expect(localStorage.getItem('surakshasaathi.authToken')).toBe('header.payload.signature');
  });

  it('clears the stored token', () => {
    setToken('t');
    clearToken();
    expect(getToken()).toBeNull();
    expect(localStorage.getItem('surakshasaathi.authToken')).toBeNull();
  });

  it('treats an empty string as signed out', () => {
    setToken('');
    expect(getToken()).toBeNull();
  });
});
