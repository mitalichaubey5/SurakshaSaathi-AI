/**
 * Simple unordered list used for safe actions and prohibited actions.
 */
export default function ActionList({ icon, title, items, prefix = '' }) {
  if (!Array.isArray(items) || items.length === 0) return null;

  return (
    <section className="rounded-3xl border border-slate-300 bg-white p-5 md:p-6">
      <h3 className="text-2xl font-extrabold text-balance">
        <span aria-hidden="true" className="mr-2">{icon}</span>
        {title}
      </h3>
      <ul className="mt-3 space-y-3">
        {items.map((item, index) => (
          <li key={`${index}-${item}`} className="flex items-start gap-3 text-xl leading-relaxed">
            <span aria-hidden="true" className="mt-1.5 shrink-0 font-bold">{prefix === '' ? icon : prefix}</span>
            <span>{item}</span>
          </li>
        ))}
      </ul>
    </section>
  );
}