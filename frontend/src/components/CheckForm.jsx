import { useState } from 'react';
import Button from './Button';
import ImageUpload from './ImageUpload';
import StatusMessage from './StatusMessage';

/**
 * The "Check This" input screen: textarea for a suspicious message/URL plus
 * the screenshot upload. Client-side validation happens here; API calls are
 * delegated to the parent (App) via callbacks.
 *
 * `apiError` is the friendly server/network error message from the parent.
 * It is rendered with an explicit "Try Again" action that safely re-submits
 * the same input (text or image) — the user never has to re-type anything.
 */
export default function CheckForm({ t, busy, apiError, onAnalyzeText, onAnalyzeImage }) {
  const [text, setText] = useState('');
  const [file, setFile] = useState(null);
  const [error, setError] = useState('');
  const [lastSubmit, setLastSubmit] = useState(null); // 'text' | 'image'

  function submitText() {
    setError('');
    if (!text.trim()) {
      setError(t.emptyInputError);
      return false;
    }
    setLastSubmit('text');
    onAnalyzeText(text.trim());
    return true;
  }

  function submitImage() {
    setError('');
    if (!file) {
      setError(t.chooseFileFirst);
      return false;
    }
    setLastSubmit('image');
    onAnalyzeImage(file);
    return true;
  }

  /** Safe retry: re-submit whatever failed last (or the text if nothing yet). */
  function retryLast() {
    if (lastSubmit === 'image') {
      submitImage();
    } else {
      submitText();
    }
  }

  function handleFileReady(chosen) {
    setFile(chosen);
  }

  const busyText = busy === 'image' ? t.readingScreenshot : t.checking;

  return (
    <section aria-labelledby="check-title" className="rounded-3xl border border-sky-200 bg-white p-5 shadow-sm md:p-7">
      <h2 id="check-title" className="text-2xl font-extrabold text-balance">{t.checkTitle}</h2>

      <form onSubmit={(e) => { e.preventDefault(); submitText(); }} className="mt-4">
        <label htmlFor="message-input" className="block text-lg font-bold">
          {t.inputLabel}
        </label>
        <textarea
          id="message-input"
          name="message"
          rows={5}
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder={t.inputPlaceholder}
          disabled={busy !== null}
          className="mt-2 w-full rounded-2xl border-2 border-slate-400 bg-white p-4 text-xl leading-relaxed focus:border-sky-700"
        />

        <div className="mt-4">
          <Button type="submit" variant="primary" busy={busy === 'text'} disabled={busy !== null}>
            {busy === 'text' ? busyText : t.analyzeButton}
          </Button>
        </div>
      </form>

      <div className="my-5 flex items-center gap-3" aria-hidden="true">
        <span className="h-px flex-1 bg-slate-300" />
        <span className="text-xl font-bold text-slate-500">{t.or}</span>
        <span className="h-px flex-1 bg-slate-300" />
      </div>

      <ImageUpload t={t} busy={busy === 'image'} onFileReady={handleFileReady} />

      <div className="mt-5">
        <Button
          variant="outline"
          onClick={submitImage}
          busy={busy === 'image'}
          disabled={busy !== null}
          className="sm:w-full"
        >
          {busy === 'image' ? busyText : t.analyzeButton}
        </Button>
      </div>

      {error && <div className="mt-4"><StatusMessage tone="error">{error}</StatusMessage></div>}

      {apiError && (
        <div className="mt-4 space-y-3">
          <StatusMessage tone="error">{apiError}</StatusMessage>
          <Button variant="primary" type="button" onClick={retryLast} disabled={busy !== null} className="sm:w-full">
            🔄 {t.tryAgain}
          </Button>
        </div>
      )}
    </section>
  );
}