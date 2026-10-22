# AI Scam-Analysis Integration

## Provider Status
- **Current Implementation:** OpenAI-compatible HTTP client (`OpenAiCompatibleLlmClient`) using `RestClient`.
- **Configured Provider:** `openai` (compatible with other OpenAI-style endpoints via `base-url`).
- **Model (default):** `gpt-4o-mini`
- **Real network requests:** Made when enabled AND `api-key` is set; otherwise safely falls back to rule engine.
- **Live connectivity tested:** Not performed in this environment (no real API key used). Claims of live connectivity would be inaccurate.

## Configuration (Environment Variables)
| Variable | Default | Required? | Notes |
|---|---|---|---|
| `OPENAI_API_KEY` | (empty) | No (fallback works) | Never logged or returned. Keep in backend env only. |
| `APP_LLM_ENABLED` | `true` | No | Set to `false` to force rule-only. |
| `APP_LLM_BASE_URL` | `https://api.openai.com/v1` | No | For OpenRouter/vllm/etc., change this. |
| `APP_LLM_MODEL` | `gpt-4o-mini` | No | Any OpenAI-compatible model. |
| `APP_LLM_TIMEOUT_SECONDS` | `20` | No | Read timeout; connect timeout 5s. |

Also see `application.yml` under `app.llm.*`.

## Risk-Combination & Fallback
- Rule-based detection runs first. LLM verdict is optional.
- **`combineRisk(ruleRisk, llmRisk)`:** `finalRisk = maxOrdinal(ruleRisk, llmRisk)`. The LLM **cannot** downgrade a strong rule-based HIGH/MEDIUM risk to LOWER ordinal; it can only raise or match.
- If LLM disabled/missing key/throws/returns invalid JSON/missing required fields → `analysisSource` becomes `"RULE_ENGINE"` and bilingual fallbacks are used.
- When LLM succeeds, `analysisSource` becomes `"RULE_ENGINE + LLM"`. LLM text is used if present; lists fallback to rule-based safe guidance if empty.

## Privacy & Safety
- No analyzed message text, OCR text, screenshots, credentials, tokens, or sensitive data logged.
- Provider requests go only to backend; credentials never exposed to frontend.
- Structured verdict validated before use; malformed JSON, missing fields, timeouts, provider errors handled safely.
- JWT auth, authorization, CORS, validation preserved.

## Testing Without Credentials
- Default behavior: without valid key, app returns rule-engine results transparently (no fabricated AI output).
- Unit tests mock `LlmClient` (no real HTTP calls). Tests: valid structured response, malformed, missing required fields, missing key, disabled, code-fence parsing, timeout/failure handling via exceptions, rule-engine HIGH cannot be downgraded.

## Cost Notes
- `gpt-4o-mini` may be free on some tiers or paid depending on provider; do **not** assume free. If using OpenRouter/self-hosted, costs vary. Live connectivity not verified.
- No retries configured (bounded timeouts) to avoid unintended cost/latency.

## Current Validation
- Backend: `mvn test` → 48 tests passed; `mvn package` → BUILD SUCCESS.
- Frontend: `npm test` → 5 files, 19 tests passed; `npm run build` → SUCCESS.
- Environment: Java 17, Spring Boot 3.x. MySQL runtime not tested.