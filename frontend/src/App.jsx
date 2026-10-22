import { useEffect, useState } from 'react';
import Header from './components/Header';
import CheckForm from './components/CheckForm';
import ResultsView from './components/ResultsView';
import EmergencyScreen from './components/EmergencyScreen';
import TrustedPersonModal from './components/TrustedPersonModal';
import AuthScreen from './components/AuthScreen';
import Button from './components/Button';
import StatusMessage from './components/StatusMessage';
import { stringsFor } from './i18n/strings';
import {
  analyzeText,
  analyzeImage,
  sendTrustedAlert,
  getEmergencyGuideline,
} from './api/surakshaApi';
import { login, register, me } from './api/authApi';
import { getToken, setToken, clearToken } from './api/tokenStore';
import { ErrorCode } from './api/http';

/**
 * SurakshaSaathi AI — elderly-first safety companion.
 * Auth gate: the user signs in first; the JWT then protects the analysis and
 * trusted-circle endpoints. Screens once signed in: 'check' (input), 'result'
 * (analysis) and 'emergency' (guidance). All API calls go through src/api;
 * components stay dumb.
 */
export default function App() {
  const [lang, setLang] = useState('en');
  const [screen, setScreen] = useState('check'); // 'check' | 'result' | 'emergency'
  const [analysis, setAnalysis] = useState(null);
  const [busy, setBusy] = useState(null); // null | 'text' | 'image'
  const [error, setError] = useState('');
  const [trustedOpen, setTrustedOpen] = useState(false);
  const [lastInput, setLastInput] = useState('');

  // Emergency guidance state
  const [guideline, setGuideline] = useState(null);
  const [emergencyBusy, setEmergencyBusy] = useState(false);
  const [emergencyError, setEmergencyError] = useState('');

  // Authentication state
  const [user, setUser] = useState(null);
  const [authChecking, setAuthChecking] = useState(true);
  const [authBusy, setAuthBusy] = useState(false);
  const [authError, setAuthError] = useState('');

  const t = stringsFor(lang);

  // Restore a persisted session on first load (token in localStorage -> /auth/me).
  useEffect(() => {
    let cancelled = false;

    async function restoreSession() {
      if (!getToken()) {
        if (!cancelled) setAuthChecking(false);
        return;
      }
      try {
        const res = await me();
        if (!cancelled) setUser(res.data);
      } catch {
        clearToken();
        if (!cancelled) setUser(null);
      } finally {
        if (!cancelled) setAuthChecking(false);
      }
    }

    restoreSession();
    return () => {
      cancelled = true;
    };
  }, []);

  function describeError(err) {
    if (err && err.code === ErrorCode.NETWORK) return t.networkError;
    if (err && err.code === ErrorCode.UNAUTHORIZED) return t.sessionExpired;
    if (err && err.code === ErrorCode.VALIDATION) return t.validationError;
    if (err && err.message) return err.message; // backend already writes friendly text
    return t.unexpectedError;
  }

  /** Sign-in/up errors should surface the backend message (e.g. "Invalid email or password"). */
  function describeAuthError(err) {
    if (err && err.code === ErrorCode.NETWORK) return t.networkError;
    if (err && err.code === ErrorCode.VALIDATION) return t.validationError;
    if (err && err.message) return err.message;
    return t.unexpectedError;
  }

  /** If a session expired mid-action, drop the user back to the sign-in screen. */
  function handleSessionExpired() {
    clearToken();
    setUser(null);
    setAuthError(t.sessionExpired);
  }

  function describeApiError(err) {
    if (err && err.code === ErrorCode.UNAUTHORIZED) {
      handleSessionExpired();
    }
    return describeError(err);
  }

  // ---------------------------------------------------------------
  // Authentication
  // ---------------------------------------------------------------
  function applyAuthResponse(res) {
    const { accessToken, user: profile } = res.data;
    setToken(accessToken);
    setUser(profile);
  }

  async function handleLogin(credentials) {
    setAuthBusy(true);
    setAuthError('');
    try {
      const res = await login(credentials);
      applyAuthResponse(res);
      return true;
    } catch (e) {
      setAuthError(describeAuthError(e));
      return false;
    } finally {
      setAuthBusy(false);
    }
  }

  async function handleRegister(details) {
    setAuthBusy(true);
    setAuthError('');
    try {
      const res = await register(details);
      applyAuthResponse(res);
      return true;
    } catch (e) {
      setAuthError(describeAuthError(e));
      return false;
    } finally {
      setAuthBusy(false);
    }
  }

  function handleLogout() {
    clearToken();
    setUser(null);
    setAuthError('');
    setAnalysis(null);
    setError('');
    setLastInput('');
    setTrustedOpen(false);
    setScreen('check');
  }

  // ---------------------------------------------------------------
  // Analysis (requires auth)
  // ---------------------------------------------------------------
  async function handleAnalyzeText(text) {
    setError('');
    setBusy('text');
    setLastInput(text);
    try {
      const res = await analyzeText(text, lang);
      setAnalysis(res.data);
      setScreen('result');
    } catch (e) {
      setError(describeApiError(e));
    } finally {
      setBusy(null);
    }
  }

  async function handleAnalyzeImage(file) {
    setError('');
    setBusy('image');
    try {
      const res = await analyzeImage(file, lang);
      setLastInput(file.name);
      setAnalysis(res.data);
      setScreen('result');
    } catch (e) {
      setError(describeApiError(e));
    } finally {
      setBusy(null);
    }
  }

  function handleCheckAnother() {
    setScreen('check');
    setAnalysis(null);
    setError('');
  }

  async function handleSendAlert(payload) {
    try {
      await sendTrustedAlert({
        contactName: payload.contactName,
        contactPhone: payload.contactPhone,
        suspiciousContent: lastInput || (analysis ? analysis.simpleExplanation : ''),
        riskLevel: analysis ? analysis.riskLevel : null,
        language: lang,
      });
    } catch (e) {
      if (e && e.code === ErrorCode.UNAUTHORIZED) {
        handleSessionExpired();
      }
      throw e;
    }
  }

  // ---------------------------------------------------------------
  // Emergency guidance (public GET /api/v1/emergency/guideline)
  // ---------------------------------------------------------------
  async function loadEmergency() {
    setEmergencyBusy(true);
    try {
      const res = await getEmergencyGuideline(lang);
      setGuideline(res.data);
      setEmergencyError('');
    } catch {
      setEmergencyError(t.emergencyError);
      setGuideline(null);
    } finally {
      setEmergencyBusy(false);
    }
  }

  function openEmergency() {
    setScreen('emergency');
    loadEmergency();
  }

  function handleBackHome() {
    setScreen('check');
    setError('');
    setEmergencyError('');
    setGuideline(null);
  }

  // Keep the emergency guidance in sync when the user switches language there.
  useEffect(() => {
    if (screen === 'emergency' && user) {
      loadEmergency();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [lang]);

  // ---------------------------------------------------------------
  // Main content
  // ---------------------------------------------------------------
  function renderMain() {
    if (authChecking) {
      return <StatusMessage tone="info" role="status">{t.authBusy}</StatusMessage>;
    }

    if (!user) {
      return (
        <AuthScreen
          t={t}
          busy={authBusy}
          error={authError}
          onLogin={handleLogin}
          onRegister={handleRegister}
        />
      );
    }

    if (screen === 'emergency') {
      return (
        <EmergencyScreen
          t={t}
          guideline={guideline}
          busy={emergencyBusy}
          error={emergencyError}
          onRetry={loadEmergency}
          onBack={handleBackHome}
        />
      );
    }

    if (screen === 'result') {
      return (
        <>
          {error && <div className="mb-4"><StatusMessage tone="error">{error}</StatusMessage></div>}

          {analysis ? (
            <ResultsView
              t={t}
              analysis={analysis}
              onCheckAnother={handleCheckAnother}
              onAskTrusted={() => setTrustedOpen(true)}
              onEmergency={openEmergency}
            />
          ) : (
            <StatusMessage tone="info">{t.unexpectedError}</StatusMessage>
          )}
        </>
      );
    }

    return (
      <div className="space-y-5">
        <nav aria-label="Main actions" className="flex flex-col gap-3 sm:flex-row">
          <Button variant="primary" className="flex-1" disabled={busy !== null}>
            🔍 {t.primaryAction}
          </Button>
          <Button
            variant="secondary"
            className="flex-1"
            onClick={() => setTrustedOpen(true)}
            disabled={busy !== null}
          >
            🧑‍🤝‍🧑 {t.secondaryAction}
          </Button>
        </nav>

        <CheckForm
          t={t}
          busy={busy}
          apiError={error}
          onAnalyzeText={handleAnalyzeText}
          onAnalyzeImage={handleAnalyzeImage}
        />

        <section aria-labelledby="how-title" className="rounded-3xl border border-sky-200 bg-white p-5">
          <h2 id="how-title" className="text-2xl font-extrabold">{t.howItWorksTitle}</h2>
          <ol className="mt-3 list-decimal space-y-2 pl-6 text-xl leading-relaxed text-slate-700">
            <li>{t.howStep1}</li>
            <li>{t.howStep2}</li>
            <li>{t.howStep3}</li>
          </ol>
          <p className="mt-4 text-lg font-medium text-slate-700">📞 {t.emergencyHint}</p>
        </section>

        <Button variant="danger" onClick={openEmergency} disabled={busy !== null} className="sm:w-full">
          🆘 {t.emergencyButton}
        </Button>
      </div>
    );
  }

  return (
    <div className="min-h-screen">
      <Header t={t} lang={lang} onLangChange={setLang} user={user} onLogout={handleLogout} />

      <main className="mx-auto max-w-2xl px-4 py-6 md:py-8">{renderMain()}</main>

      <TrustedPersonModal
        open={trustedOpen && !!user}
        onClose={() => setTrustedOpen(false)}
        t={t}
        suspiciousContent={lastInput}
        riskLevel={analysis ? analysis.riskLevel : null}
        onSend={handleSendAlert}
      />

      <footer className="py-6 text-center text-base text-slate-500">
        {t.appName} — {t.slogan}
      </footer>
    </div>
  );
}
