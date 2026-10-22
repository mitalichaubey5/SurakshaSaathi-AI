/**
 * Risk banner. Risk is communicated with ALL of: emoji, text label, border,
 * background colour and a written description — never colour alone.
 */
const PANEL = {
  LOW: 'border-green-700 bg-green-50 text-green-900',
  MEDIUM: 'border-amber-600 bg-amber-50 text-amber-900',
  HIGH: 'border-red-700 bg-red-50 text-red-900',
};

export default function RiskBadge({ riskLevel, riskLabel, riskEmoji, t }) {
  const panel = PANEL[riskLevel] || PANEL.MEDIUM;
  const description =
    riskLevel === 'HIGH'
      ? t.riskHighDesc
      : riskLevel === 'LOW'
        ? t.riskLowDesc
        : t.riskMediumDesc;

  return (
    <div
      role="status"
      className={`flex items-center gap-5 rounded-3xl border-4 px-6 py-6 ${panel}`}
    >
      <span aria-hidden="true" className="text-6xl md:text-7xl">{riskEmoji}</span>
      <div>
        <p className="text-4xl font-black leading-tight">{riskLabel}</p>
        <p className="mt-1 text-lg font-medium text-balance">{description}</p>
      </div>
    </div>
  );
}