const VARIANTS = {
  primary:
    'bg-sky-700 text-white hover:bg-sky-800 active:bg-sky-900 border-b-4 border-sky-900',
  secondary:
    'bg-amber-500 text-slate-900 hover:bg-amber-600 active:bg-amber-700 border-b-4 border-amber-800',
  outline:
    'bg-white text-sky-800 border-2 border-sky-700 hover:bg-sky-100 active:bg-sky-200',
  subtle:
    'bg-sky-100 text-sky-900 hover:bg-sky-200 active:bg-sky-300',
  danger:
    'bg-red-700 text-white hover:bg-red-800 active:bg-red-900 border-b-4 border-red-900',
};

/**
 * Large, touch-friendly button (min-height so older users can aim easily).
 */
export default function Button({ variant = 'primary', className = '', disabled = false, busy = false, children, ...props }) {
  const base =
    'inline-flex min-h-14 w-full items-center justify-center gap-2 rounded-2xl px-6 py-4 text-xl font-bold ' +
    'disabled:cursor-not-allowed disabled:opacity-60 transition-colors sm:w-auto';
  return (
    <button className={`${base} ${VARIANTS[variant]} ${className}`} disabled={disabled || busy} {...props}>
      <span aria-hidden="true" className="text-2xl">{busy ? '⏳' : ''}</span>
      {children}
    </button>
  );
}