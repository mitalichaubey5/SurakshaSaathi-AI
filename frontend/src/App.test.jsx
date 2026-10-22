import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import App from './App';
import { clearToken, getToken } from './api/tokenStore';

const envelope = (data, message = 'ok') =>
  JSON.stringify({ success: true, message, data, timestamp: '2026-10-08T00:00:00Z' });

const jsonResponse = (body, status = 200) =>
  new Response(body, { status, headers: { 'Content-Type': 'application/json' } });

beforeEach(() => clearToken());
afterEach(() => {
  vi.unstubAllGlobals();
  clearToken();
});

describe('App authentication gate', () => {
  it('shows the sign-in screen when there is no stored token', async () => {
    render(<App />);
    expect(await screen.findByRole('tab', { name: 'Sign In' })).toBeInTheDocument();
  });

  it('signs in, stores the token and reveals the safety-check screen', async () => {
    const fetchMock = vi.fn(async (url) => {
      if (url === '/api/v1/auth/login') {
        return jsonResponse(
          envelope({
            accessToken: 'jwt-token',
            tokenType: 'Bearer',
            expiresIn: 3600,
            user: { id: 1, name: 'Sunita', email: 's@b.com', role: 'USER' },
          }),
        );
      }
      return jsonResponse(envelope({}));
    });
    vi.stubGlobal('fetch', fetchMock);

    render(<App />);

    await screen.findByRole('tab', { name: 'Sign In' });
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 's@b.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'secret1' } });
    fireEvent.click(screen.getByRole('button', { name: 'Sign In' }));

    expect(await screen.findByRole('button', { name: /Check This/ })).toBeInTheDocument();
    expect(screen.getByText(/Sunita/)).toBeInTheDocument();
    expect(getToken()).toBe('jwt-token');
  });

  it('keeps the user on the sign-in screen when the password is wrong', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(jsonResponse(JSON.stringify({ success: false, message: 'Invalid email or password', data: null }), 401));
    vi.stubGlobal('fetch', fetchMock);

    render(<App />);

    await screen.findByRole('tab', { name: 'Sign In' });
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 's@b.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'wrong1' } });
    fireEvent.click(screen.getByRole('button', { name: 'Sign In' }));

    expect(await screen.findByText('Invalid email or password')).toBeInTheDocument();
    expect(getToken()).toBeNull();
  });

  it('restores a session from a persisted token via /auth/me', async () => {
    localStorage.setItem('surakshasaathi.authToken', 'persisted');
    const fetchMock = vi.fn(async (url) => {
      if (url === '/api/v1/auth/me') {
        return jsonResponse(envelope({ id: 7, name: 'Ravi', email: 'r@b.com', role: 'USER' }));
      }
      return jsonResponse(envelope({}));
    });
    vi.stubGlobal('fetch', fetchMock);

    render(<App />);

    expect(await screen.findByRole('button', { name: /Check This/ })).toBeInTheDocument();
    expect(screen.getByText(/Ravi/)).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/auth/me', expect.objectContaining({ headers: { Authorization: 'Bearer persisted' } }));
  });
});
