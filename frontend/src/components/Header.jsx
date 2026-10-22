import LanguageToggle from './LanguageToggle';
import Button from './Button';

/**
 * Top bar: app name, language switch, and — once signed in — the user's name
 * plus a Sign Out action.
 */
export default function Header({ t, lang, onLangChange, user, onLogout }) {
  return (
    <header className="bg-sky-900 text-white">
      <div className="mx-auto max-w-2xl px-4 py-4">
        <div className="flex items-center justify-between gap-4">
          <div>
            <p className="text-xl font-extrabold">{t.appName}</p>
            <p className="text-base text-sky-200">{t.slogan}</p>
          </div>
          <LanguageToggle lang={lang} onLangChange={onLangChange} />
        </div>

        {user && (
          <div className="mt-3 flex flex-wrap items-center justify-between gap-3 border-t border-sky-700 pt-3">
            <p className="text-lg font-bold">
              <span aria-hidden="true" className="mr-1">🙏</span>
              {t.greeting} {user.name}
            </p>
            <Button variant="subtle" onClick={onLogout} className="!min-h-11 !px-4 !py-2 !text-lg">
              {t.logout}
            </Button>
          </div>
        )}
      </div>
    </header>
  );
}
