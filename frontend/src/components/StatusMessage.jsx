/**
 * Accessible status box. Announce every change to assistive technology via
 * aria-live. Tone styles are paired with an icon so meaning never depends on
 * colour alone.
 */
export default function StatusMessage({ tone = 'error', children, role = 'alert' }) {
  const styles = {
    error: 'border-red-700 bg-red-50 text-red-900',
    success: 'border-green-700 bg-green-50 text-green-900',
    info: 'border-sky-700 bg-sky-50 text-sky-900',
  }[tone];

  const icon = { error: '⚠️', success: '✅', info: 'ℹ️' }[tone];

  return (
    <div role={role} aria-live="polite" className={`rounded-2xl border-2 px-5 py-4 text-xl font-medium ${styles}`}>
      <span aria-hidden="true" className="mr-2 text-2xl">{icon}</span>
      {children}
    </div>
  );
}