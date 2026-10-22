import { LANGS } from '../i18n/strings';

/**
 * English / Simple Hindi toggle. Uses aria-pressed so screen readers know
 * which language is active. Devanagari is shown for the Hindi option.
 */
export default function LanguageToggle({ lang, onLangChange }) {
  const option = (code, label) => {
    const active = lang === code;
    return (
      <button
        type="button"
        key={code}
        aria-pressed={active}
        onClick={() => onLangChange(code)}
        className={`min-h-12 rounded-xl px-4 py-2 text-lg font-bold transition-colors ${
          active ? 'bg-white text-sky-900 shadow-sm' : 'bg-sky-800 text-white hover:bg-sky-700'
        }`}
      >
        {label}
      </button>
    );
  };

  return (
    <div
      role="group"
      aria-label="Language / Bhasha"
      className="flex gap-2 rounded-2xl bg-sky-800 p-1.5"
    >
      {option(LANGS.EN, 'English')}
      {option(LANGS.HI, 'हिंदी')}
    </div>
  );
}