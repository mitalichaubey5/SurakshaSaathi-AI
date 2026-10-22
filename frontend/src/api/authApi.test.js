import { afterEach, describe, expect, it, vi } from 'vitest';
import { register, login, me } from './authApi';
import { setToken, clearToken } from './tokenStore';

const okEnvelope = (data) =>
  JSON.stringify({ success: true, message: 'ok', data, timestamp: '2026-10-08T00:00:00Z' });

const authPayload = {
  accessToken: 'jwt-token',
  tokenType: 'Bearer',
  expiresIn: 3600,
  user: { id: 1, name: 'Sunita', email: 's@b.com', role: 'USER' },
};

afterEach(() => {
  vi.unstubAllGlobals();
  clearToken();
});

describe('authApi', () => {
  it('register posts name/email/password to /api/v1/auth/register', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(new Response(okEnvelope(authPayload), { status: 200, headers: { 'Content-Type': 'application/json' } }));
    vi.stubGlobal('fetch', fetchMock);

    const res = await register({ name: 'Sunita', email: 's@b.com', password: 'secret1' });

    const [url, options] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/v1/auth/register');
    expect(options.method).toBe('POST');
    expect(JSON.parse(options.body)).toEqual({ name: 'Sunita', email: 's@b.com', password: 'secret1' });
    expect(res.data.accessToken).toBe('jwt-token');
  });

  it('login posts email/password to /api/v1/auth/login', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(new Response(okEnvelope(authPayload), { status: 200, headers: { 'Content-Type': 'application/json' } }));
    vi.stubGlobal('fetch', fetchMock);

    await login({ email: 's@b.com', password: 'secret1' });

    const [url, options] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/v1/auth/login');
    expect(JSON.parse(options.body)).toEqual({ email: 's@b.com', password: 'secret1' });
  });

  it('me sends the stored Bearer token', async () => {
    setToken('stored-token');
    const fetchMock = vi
      .fn()
      .mockResolvedValue(
        new Response(okEnvelope({ id: 1, name: 'Sunita', email: 's@b.com', role: 'USER' }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      );
    vi.stubGlobal('fetch', fetchMock);

    const res = await me();

    const [url, options] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/v1/auth/me');
    expect(options.headers.Authorization).toBe('Bearer stored-token');
    expect(res.data.name).toBe('Sunita');
  });

  it('maps invalid credentials (401) onto an UNAUTHORIZED error', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ success: false, message: 'Invalid email or password', data: null }), {
        status: 401,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    await expect(login({ email: 's@b.com', password: 'wrong' })).rejects.toMatchObject({
      code: 'UNAUTHORIZED',
      message: 'Invalid email or password',
    });
  });
});
