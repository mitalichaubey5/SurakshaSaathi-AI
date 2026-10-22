import RiskBadge from './RiskBadge';
import Button from './Button';
import ActionList from './ActionList';

function Section({ icon, title, children }) {
  return (
    <section className="rounded-3xl border border-slate-300 bg-white p-5 md:p-6">
      <h3 className="text-2xl font-extrabold text-balance">
        <span aria-hidden="true" className="mr-2">{icon}</span>
        {title}
      </h3>
      <div className="mt-3 text-xl leading-relaxed">{children}</div>
    </section>
  );
}

/**
 * Analysis result screen: risk level, plain-language "why", safe actions,
 * prohibited actions and verification steps — all in one scrollable card,
 * matching the backend AnalysisResponse structure.
 */
export default function ResultsView({ t, analysis, onCheckAnother, onAskTrusted, onEmergency }) {
  const hasSignals = Array.isArray(analysis.matchedSignals) && analysis.matchedSignals.length > 0;

  return (
    <main aria-live="polite" className="space-y-4">
      <RiskBadge
        riskLevel={analysis.riskLevel}
        riskLabel={analysis.riskLabel}
        riskEmoji={analysis.riskEmoji}
        t={t}
      />

      <Section icon="🔍" title={t.whySuspicious}>
        <p>{analysis.simpleExplanation}</p>
      </Section>

      <ActionList
        icon="✅"
        title={t.safeActionsTitle}
        items={analysis.safeActions}
        prefix=""
      />

      <ActionList
        icon="🚫"
        title={t.prohibitedActionsTitle}
        items={analysis.prohibitedActions}
        prefix="•"
      />

      <Section icon="🏦" title={t.verificationTitle}>
        <p>{analysis.verificationSteps}</p>
      </Section>

      {hasSignals && (
        <Section icon="👀" title={t.matchedSignalsTitle}>
          <ul className="flex flex-wrap gap-2">
            {analysis.matchedSignals.map((signal) => (
              <li key={signal} className="rounded-full bg-slate-100 px-4 py-2 text-lg font-semibold text-slate-700">
                {signal}
              </li>
            ))}
          </ul>
        </Section>
      )}

      <div className="flex flex-col gap-3 sm:flex-row">
        <Button variant="secondary" onClick={onAskTrusted} className="flex-1">
          🧑‍🤝‍🧑 {t.trustedTitle}
        </Button>
        <Button variant="outline" onClick={onCheckAnother} className="flex-1">
          {t.checkAnother}
        </Button>
      </div>

      <Button variant="danger" onClick={onEmergency} className="sm:w-full">
        🆘 {t.emergencyButton}
      </Button>

      <p className="pt-2 text-center text-lg font-medium text-slate-700">
        📞 {t.emergencyHint}
      </p>
    </main>
  );
}