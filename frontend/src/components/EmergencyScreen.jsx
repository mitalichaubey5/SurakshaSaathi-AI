import Button from './Button';
import StatusMessage from './StatusMessage';

/**
 * Emergency guidance screen — backed by GET /api/v1/emergency/guideline.
 *
 * - Helplines are rendered as large `tel:` links (1930 / 112 / 181).
 * - The official complaint portal is linked openly, with a clear warning to
 *   type the address manually instead of using links from suspicious messages.
 * - Has explicit loading, error + retry, and back states.
 */
export default function EmergencyScreen({ t, guideline, busy, error, onRetry, onBack }) {
  if (busy) {
    return (
      <main className="space-y-4">
        <StatusMessage tone="info" role="status">⏳ {t.emergencyLoading}</StatusMessage>
        <Button variant="outline" onClick={onBack} className="sm:w-full">← {t.emergencyBack}</Button>
      </main>
    );
  }

  if (error || !guideline) {
    return (
      <main className="space-y-4">
        <StatusMessage tone="error">{error || t.emergencyError}</StatusMessage>
        <div className="flex flex-col gap-3 sm:flex-row">
          <Button variant="primary" onClick={onRetry} className="flex-1">🔄 {t.tryAgain}</Button>
          <Button variant="outline" onClick={onBack} className="flex-1">← {t.emergencyBack}</Button>
        </div>
      </main>
    );
  }

  return (
    <main className="space-y-4" aria-live="polite">
      <div className="rounded-3xl border-4 border-red-700 bg-red-700 p-6 text-white">
        <h2 className="text-3xl font-black text-balance">🆘 {t.emergencyTitle}</h2>
        <p className="mt-2 text-xl font-medium text-balance">{guideline.headline}</p>
      </div>

      <section className="rounded-3xl border border-slate-300 bg-white p-5 md:p-6">
        <h3 className="text-2xl font-extrabold text-balance">📞 {t.emergencyHelplineTitle}</h3>
        <ul className="mt-4 space-y-4">
          {guideline.helplines.map((helpline) => (
            <li key={helpline.number} className="rounded-2xl border border-slate-300 bg-sky-50 p-4">
              <a
                href={`tel:${String(helpline.number).replace(/[^0-9+]/g, '')}`}
                className="flex flex-wrap items-baseline gap-3 rounded-xl text-sky-900 hover:underline"
              >
                <span className="text-4xl font-black">{helpline.number}</span>
                <span className="text-xl font-bold">{helpline.name}</span>
              </a>
              <p className="mt-2 text-lg leading-relaxed text-slate-700">{helpline.whenToUse}</p>
            </li>
          ))}
        </ul>
      </section>

      <section className="rounded-3xl border border-slate-300 bg-white p-5 md:p-6">
        <h3 className="text-2xl font-extrabold text-balance">{t.emergencyStepsTitle}</h3>
        <ul className="mt-3 space-y-3">
          {guideline.immediateSteps.map((step, index) => (
            <li key={index} className="flex items-start gap-3 text-xl leading-relaxed">
              <span aria-hidden="true" className="mt-1 shrink-0 font-black">✅</span>
              <span>{step}</span>
            </li>
          ))}
        </ul>
      </section>

      <section className="rounded-3xl border border-slate-300 bg-white p-5 md:p-6">
        <h3 className="text-2xl font-extrabold text-balance">🌐 {t.emergencyReportTitle}</h3>
        <p className="mt-3 text-xl">{t.emergencyReportHint}</p>
        <a
          href={guideline.reportUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="mt-4 inline-block min-h-14 rounded-2xl border-b-4 border-sky-900 bg-sky-700 px-6 py-4 text-xl font-bold text-white no-underline hover:bg-sky-800"
        >
          {t.openReportSite}
        </a>
        <p className="mt-4 rounded-2xl bg-red-50 p-4 text-lg font-medium text-red-900 border-2 border-red-200">
          ⚠️ {t.emergencyTypeHint}
        </p>
      </section>

      <div className="flex flex-col gap-3 sm:flex-row">
        <Button variant="primary" onClick={onBack} className="flex-1">← {t.emergencyBack}</Button>
      </div>
    </main>
  );
}