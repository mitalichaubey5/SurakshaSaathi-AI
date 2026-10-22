import { useRef, useState } from 'react';
import StatusMessage from './StatusMessage';

const ACCEPTED = ['image/png', 'image/jpeg', 'image/webp'];
const MAX_BYTES = 5 * 1024 * 1024; // backend max-file-size is 5 MB

/**
 * Screenshot upload control.
 * - validates type (PNG/JPG/WebP) and size (<= 5 MB)
 * - shows the chosen image name + a preview
 * - stays ready for the backend OCR pipeline (currently a placeholder)
 */
export default function ImageUpload({ t, onFileReady, busy }) {
  const inputRef = useRef(null);
  const [file, setFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState('');
  const [error, setError] = useState('');

  function handleChange(event) {
    const chosen = event.target.files && event.target.files[0];
    setError('');
    if (!chosen) return;

    if (!ACCEPTED.includes(chosen.type)) {
      event.target.value = '';
      setError(t.fileTypeError);
      return;
    }
    if (chosen.size > MAX_BYTES) {
      event.target.value = '';
      setError(t.fileSizeError);
      return;
    }

    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setFile(chosen);
    setPreviewUrl(URL.createObjectURL(chosen));
    onFileReady(chosen);
  }

  function handleRemove() {
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setFile(null);
    setPreviewUrl('');
    if (inputRef.current) inputRef.current.value = '';
    onFileReady(null);
  }

  return (
    <div className="rounded-2xl border-2 border-dashed border-sky-700 bg-white p-4">
      <p className="text-xl font-bold">{t.uploadLabel}</p>
      <p className="mt-1 text-base text-slate-700">{t.uploadHint}</p>

      <input
        ref={inputRef}
        id="screenshot-input"
        type="file"
        accept={ACCEPTED.join(',')}
        className="sr-only"
        onChange={handleChange}
        disabled={busy}
      />

      {!file ? (
        <label
          htmlFor="screenshot-input"
          className="mt-4 flex min-h-28 w-full cursor-pointer flex-col items-center justify-center gap-1 rounded-2xl bg-sky-50 px-4 py-6 text-center text-xl font-bold text-sky-800 hover:bg-sky-100"
        >
          <span aria-hidden="true" className="text-4xl">📷</span>
          {t.chooseFile}
        </label>
      ) : (
        <div className="mt-4 flex items-center gap-4 rounded-2xl bg-sky-50 p-3">
          {previewUrl && (
            <img
              src={previewUrl}
              alt=""
              className="h-20 w-20 rounded-xl border border-sky-300 object-cover"
            />
          )}
          <div className="min-w-0 flex-1">
            <p className="truncate text-lg font-bold">{file.name}</p>
            <p className="text-base text-slate-600">
              {t.fileSelected}: {Math.round(file.size / 1024)} KB
            </p>
          </div>
          <button
            type="button"
            onClick={handleRemove}
            disabled={busy}
            className="min-h-12 rounded-xl bg-white px-4 py-2 text-lg font-bold text-red-700 border-2 border-red-300 hover:bg-red-50"
          >
            {t.removeFile}
          </button>
        </div>
      )}

      {error && <div className="mt-3"><StatusMessage tone="error">{error}</StatusMessage></div>}
    </div>
  );
}