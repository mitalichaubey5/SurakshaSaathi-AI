# SurakshaSaathi AI — Frontend (Step 3)

Elderly-first React + Tailwind UI. Communicates only with the existing Spring Boot API.

## Run

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173  (proxies /api -> http://localhost:8080)
```

Start the backend first (from `../backend`): `mvn spring-boot:run`.

## Scripts

| Command | Purpose |
|---|---|
| `npm run dev` | Vite dev server with `/api` proxy to the backend |
| `npm run build` | Production build to `dist/` |
| `npm run preview` | Preview the production build |
| `npm test` | Vitest suite |

## Architecture

- `src/api/` — HTTP client + typed API calls (no UI knowledge, no secrets).
- `src/i18n/strings.js` — all user-facing strings for `en` and `hi`; add a language by adding a key map.
- `src/components/` — reusable, accessible UI components (large buttons, risk badge, result sections, trusted-person dialog, etc.).
- `src/App.jsx` — screen state, language state, API orchestration.

The image endpoint already exists in the backend (OCR is a placeholder there until a later step);
the upload UI validates type/size and shows a processing state, so it will keep working when real OCR lands.