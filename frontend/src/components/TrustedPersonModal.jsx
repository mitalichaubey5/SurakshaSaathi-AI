import { useEffect, useRef, useState } from 'react';
import Button from './Button';
import StatusMessage from './StatusMessage';

/**
 * "Ask My Trusted Person" — a simple accessible dialog. Sends the alert via
 * the backend mock trusted-circle endpoint (handled by the `onSend` callback
 * provided by the parent). Closes on Escape or by the Close button.
 */
export default function TrustedPersonModal({ open, onClose, t, suspiciousContent, riskLevel, onSend }) {
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [busy, setBusy] = useState(false);
  const [status, setStatus] = useState(null); // { tone: 'success'|'error', message }
  const nameRef = useRef(null);

  useEffect(() => {
    if (open) {
      setStatus(null);
      if (nameRef.current) nameRef.current.focus();
    }
  }, [open]);

  if (!open) return null;

  async function handleSubmit(event) {
    event.preventDefault();
    setBusy(true);
    setStatus(null);
    try {
      await onSend({ contactName: name.trim(), contactPhone: phone.trim(), riskLevel });
      setStatus({ tone: 'success', message: t.alertSent });
    } catch {
      setStatus({ tone: 'error', message: t.alertFailed });
    } finally {
      setBusy(false);
    }
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="trusted-title"
      className="fixed inset-0 z-50 flex items-end justify-center bg-slate-900/70 p-0 sm:items-center sm:p-4"
      onKeyDown={(e) => {
        if (e.key === 'Escape') onClose();
      }}
    >
      <div className="max-h-[92vh] w-full max-w-xl overflow-y-auto rounded-t-3xl bg-sky-50 p-6 sm:rounded-3xl md:p-8">
        <div className="flex items-start justify-between gap-4">
          <h2 id="trusted-title" className="text-2xl font-extrabold text-balance">
            🧑‍🤝‍🧑 {t.trustedTitle}
          </h2>
          <button
            type="button"
            onClick={onClose}
            aria-label={t.close}
            className="min-h-12 min-w-12 rounded-full border-2 border-slate-400 bg-white text-2xl font-bold leading-none hover:bg-slate-100"
          >
            ✕
          </button>
        </div>

        <p className="mt-2 text-xl text-slate-700">{t.trustedHint}</p>

        {suspiciousContent && (
          <p className="mt-4 rounded-2xl border border-slate-300 bg-white p-4 text-lg text-slate-700">
            <strong className="block text-slate-900">📩</strong>
            {suspiciousContent.length > 200 ? `${suspiciousContent.slice(0, 200)}…` : suspiciousContent}
          </p>
        )}

        <form onSubmit={handleSubmit} className="mt-5 space-y-5">
          <div>
            <label htmlFor="trusted-name" className="block text-lg font-bold">{t.trustedNameLabel}</label>
            <input
              ref={nameRef}
              id="trusted-name"
              type="text"
              required
              autoComplete="name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder={t.trustedNamePlaceholder}
              className="mt-2 w-full rounded-2xl border-2 border-slate-400 bg-white p-4 text-xl"
            />
          </div>

          <div>
            <label htmlFor="trusted-phone" className="block text-lg font-bold">{t.trustedPhoneLabel}</label>
            <input
              id="trusted-phone"
              type="tel"
              required
              autoComplete="tel"
              inputMode="numeric"
              pattern="[0-9+\s-]{7,15}"
              title="Phone number"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder={t.trustedPhonePlaceholder}
              className="mt-2 w-full rounded-2xl border-2 border-slate-400 bg-white p-4 text-xl"
            />
          </div>

          {status && (
            <div>
              <StatusMessage tone={status.tone}>{status.message}</StatusMessage>
            </div>
          )}

          <div className="flex flex-col gap-3 sm:flex-row">
            <Button type="submit" variant="primary" busy={busy} disabled={busy} className="flex-1">
              {busy ? t.sendingAlert : t.sendAlert}
            </Button>
            <Button type="button" variant="outline" onClick={onClose} disabled={busy} className="flex-1">
              {t.close}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}