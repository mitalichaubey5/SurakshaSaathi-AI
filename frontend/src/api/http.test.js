import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { getJson, postJson, postMultipart, ErrorCode } from './http';
import { getToken, setToken, clearToken } from './tokenStore';

const okEnvelope = (data) =>
  JSON.stringify({ success: true, message: 'ok', data, timestamp: '2026-10-08T00:00:00Z' });

const jsonResponse = (body, status = 200) =>
  new Response(body, { status, headers: { 'Content-Type': 'application/json' } });

beforeEach(() => clearToken());
afterEach(() => {
  vi.unstubAllGlobals();
  clearToken();
});

describe('http auth handling', () => {
  it('omits the Authorization header and options when signed out', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(okEnvelope({ ok: 1 })));
    vi.stubGlobal('fetch', fetchMock);

    await getJson('/api/v1/emergency/guideline?language=en');

    expect(fetchMock.mock.calls[0].length).toBe(1); // plain GET, unchanged behaviour
  });

  it('attaches a Bearer token to GET requests when signed in', async () => {
    setToken('tok123');
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(okEnvelope({})));
    vi.stubGlobal('fetch', fetchMock);

    await getJson('/api/v1/auth/me');

    const [url, options] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/v1/auth/me');
    expect(options.headers.Authorization).toBe('Bearer tok123');
  });

  it('attaches JSON + Bearer headers to POST requests when signed in', async () => {
    setToken('tok123');
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(okEnvelope({})));
    vi.stubGlobal('fetch', fetchMock);

    await postJson('/api/v1/analyze/text', { text: 'hi', language: 'en' });

    const [, options] = fetchMock.mock.calls[0];
    expect(options.headers['Content-Type']).toBe('application/json');
    expect(options.headers.Authorization).toBe('Bearer tok123');
    expect(JSON.parse(options.body)).toEqual({ text: 'hi', language: 'en' });
  });

  it('does not set a Content-Type for multipart and still sends the token', async () => {
    setToken('tok123');
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(okEnvelope({})));
    vi.stubGlobal('fetch', fetchMock);

    const form = new FormData();
    form.append('file', new Blob(['x'], { type: 'image/png' }), 'shot.png');
    await postMultipart('/api/v1/analyze/image', form, 'language=en');

    const [, options] = fetchMock.mock.calls[0];
    expect(options.headers['Content-Type']).toBeUndefined();
    expect(options.headers.Authorization).toBe('Bearer tok123');
  });

  it('clears the token and throws UNAUTHORIZED on a 401 response', async () => {
    setToken('expired');
    const fetchMock = vi
      .fn()
      .mockResolvedValue(jsonResponse(JSON.stringify({ success: false, message: 'Unauthorized', data: null }), 401));
    vi.stubGlobal('fetch', fetchMock);

    await expect(getJson('/api/v1/auth/me')).rejects.toMatchObject({
      code: ErrorCode.UNAUTHORIZED,
      status: 401,
    });
    expect(getToken()).toBeNull();
  });
});
