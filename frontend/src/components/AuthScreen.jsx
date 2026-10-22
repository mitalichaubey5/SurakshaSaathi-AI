import { useState } from 'react';
import Button from './Button';
import StatusMessage from './StatusMessage';

/**
 * Sign-in / create-account screen. Elderly-first: big labels, large inputs,
 * one clear action. It is a "dumb" component - the parent (App) owns the API
 * calls and passes `busy` / `error` plus the `onLogin` / `onRegister` handlers.
 */
export default function AuthScreen({ t, busy, error, onLogin, onRegister }) {
  const [mode, setMode] = useState('login'); // 'login' | 'register'
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [localError, setLocalError] = useState('');

  const isRegister = mode === 'register';
  const shownError = localError || error;

  function switchMode(next) {
    setMode(next);
    setLocalError('');
  }

  function validate() {
    if (isRegister && name.trim().length < 2) return t.authNameError;
    const emailTrim = email.trim();
    if (!emailTrim || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailTrim)) return t.authEmailError;
    if (password.length < 6) return t.authPasswordError;
    return '';
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const message = validate();
    if (message) {
      setLocalError(message);
      return;
    }
    setLocalError('');
    if (isRegister) {
      await onRegister({ name: name.trim(), email: email.trim(), password });
    } else {
      await onLogin({ email: email.trim(), password });
    }
  }

  return (
    <section
      aria-labelledby="auth-title"
      className="rounded-3xl border border-sky-200 bg-white p-5 shadow-sm md:p-7"
    >
      <h2 id="auth-title" className="text-2xl font-extrabold text-balance">{t.authTitle}</h2>
      <p className="mt-2 text-xl text-slate-700">{t.authSubtitle}</p>

      <div role="tablist" aria-label={t.authTitle} className="mt-5 flex gap-2 rounded-2xl bg-sky-100 p-1">
        {[
          { key: 'login', label: t.loginTab },
          { key: 'register', label: t.registerTab },
        ].map((tab) => (
          <button
            key={tab.key}
            type="button"
            role="tab"
            aria-selected={mode === tab.key}
            onClick={() => switchMode(tab.key)}
            className={`min-h-12 flex-1 rounded-xl px-4 text-lg font-bold transition-colors ${
              mode === tab.key ? 'bg-white text-sky-900 shadow-sm' : 'text-sky-800 hover:bg-sky-100'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <form onSubmit={handleSubmit} className="mt-5 space-y-4" noValidate>
        {isRegister && (
          <div>
            <label htmlFor="auth-name" className="block text-lg font-bold">{t.nameLabel}</label>
            <input
              id="auth-name"
              name="name"
              type="text"
              autoComplete="name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder={t.namePlaceholder}
              disabled={busy}
              className="mt-2 w-full rounded-2xl border-2 border-slate-400 bg-white p-4 text-xl focus:border-sky-700"
            />
          </div>
        )}

        <div>
          <label htmlFor="auth-email" className="block text-lg font-bold">{t.emailLabel}</label>
          <input
            id="auth-email"
            name="email"
            type="email"
            inputMode="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder={t.emailPlaceholder}
            disabled={busy}
            className="mt-2 w-full rounded-2xl border-2 border-slate-400 bg-white p-4 text-xl focus:border-sky-700"
          />
        </div>

        <div>
          <label htmlFor="auth-password" className="block text-lg font-bold">{t.passwordLabel}</label>
          <div className="mt-2 flex items-stretch gap-2">
            <input
              id="auth-password"
              name="password"
              type={showPassword ? 'text' : 'password'}
              autoComplete={isRegister ? 'new-password' : 'current-password'}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder={t.passwordPlaceholder}
              disabled={busy}
              className="w-full rounded-2xl border-2 border-slate-400 bg-white p-4 text-xl focus:border-sky-700"
            />
            <button
              type="button"
              onClick={() => setShowPassword((v) => !v)}
              aria-label={showPassword ? t.hidePassword : t.showPassword}
              className="min-h-12 rounded-2xl border-2 border-slate-400 bg-white px-4 text-lg font-bold text-slate-700 hover:bg-slate-100"
            >
              {showPassword ? '🙈' : '👁️'}
            </button>
          </div>
        </div>

        {shownError && (
          <div>
            <StatusMessage tone="error">{shownError}</StatusMessage>
          </div>
        )}

        <Button type="submit" variant="primary" busy={busy} disabled={busy} className="sm:w-full">
          {busy ? t.authBusy : isRegister ? t.registerButton : t.loginButton}
        </Button>

        <button
          type="button"
          onClick={() => switchMode(isRegister ? 'login' : 'register')}
          disabled={busy}
          className="w-full text-lg font-bold text-sky-800 underline hover:text-sky-900 disabled:opacity-60"
        >
          {isRegister ? t.toggleToLogin : t.toggleToRegister}
        </button>
      </form>
    </section>
  );
}
