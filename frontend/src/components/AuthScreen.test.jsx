import { describe, expect, it, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import AuthScreen from './AuthScreen';
import { strings } from '../i18n/strings';

const t = strings.en;

function setup(props = {}) {
  const handlers = {
    onLogin: vi.fn().mockResolvedValue(true),
    onRegister: vi.fn().mockResolvedValue(true),
  };
  render(<AuthScreen t={t} busy={false} error="" {...handlers} {...props} />);
  return handlers;
}

describe('AuthScreen', () => {
  it('shows the login form first (no name field)', () => {
    setup();
    expect(screen.getByRole('tab', { name: t.loginTab })).toHaveAttribute('aria-selected', 'true');
    expect(screen.queryByLabelText(t.nameLabel)).not.toBeInTheDocument();
  });

  it('switches to the registration form', () => {
    setup();
    fireEvent.click(screen.getByRole('tab', { name: t.registerTab }));
    expect(screen.getByLabelText(t.nameLabel)).toBeInTheDocument();
  });

  it('validates email and password before calling the API', () => {
    const handlers = setup();
    fireEvent.change(screen.getByLabelText(t.emailLabel), { target: { value: 'not-an-email' } });
    fireEvent.change(screen.getByLabelText(t.passwordLabel), { target: { value: '123' } });
    fireEvent.click(screen.getByRole('button', { name: t.loginButton }));

    expect(screen.getByText(t.authEmailError)).toBeInTheDocument();
    expect(handlers.onLogin).not.toHaveBeenCalled();
  });

  it('submits trimmed credentials on login', async () => {
    const handlers = setup();
    fireEvent.change(screen.getByLabelText(t.emailLabel), { target: { value: '  s@b.com ' } });
    fireEvent.change(screen.getByLabelText(t.passwordLabel), { target: { value: 'secret1' } });
    fireEvent.click(screen.getByRole('button', { name: t.loginButton }));

    expect(handlers.onLogin).toHaveBeenCalledWith({ email: 's@b.com', password: 'secret1' });
  });

  it('requires a name when registering', () => {
    const handlers = setup();
    fireEvent.click(screen.getByRole('tab', { name: t.registerTab }));
    fireEvent.change(screen.getByLabelText(t.emailLabel), { target: { value: 's@b.com' } });
    fireEvent.change(screen.getByLabelText(t.passwordLabel), { target: { value: 'secret1' } });
    fireEvent.click(screen.getByRole('button', { name: t.registerButton }));

    expect(screen.getByText(t.authNameError)).toBeInTheDocument();
    expect(handlers.onRegister).not.toHaveBeenCalled();
  });

  it('shows the error message passed by the parent', () => {
    setup({ error: t.sessionExpired });
    expect(screen.getByText(t.sessionExpired)).toBeInTheDocument();
  });
});
