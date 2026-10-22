# SurakshaSaathi AI — Step 2: Scam Analyzer + LLM Integration
> **"Pause. Verify. Then Trust."** · Flow: Detect → Explain → Verify → Guide → Escalate

## What Step 2 delivers
| Layer | Files |
|---|---|
| Rule engine | `service/rule/{ScamSignal,ScamRule,ScamRules,RuleResult,RuleEngine}.java` + `rule/impl/{ShortenedUrlRule,SuspiciousTldRule,LookalikeDomainRule}.java` |
| LLM service | `service/impl/LlmPromptServiceImpl.java` (OpenAI-compatible client, strict JSON, graceful fallback) |
| Analyzer | `service/impl/ScamAnalyzerServiceImpl.java` (rule + LLM safe merge, bilingual fallbacks) |
| Tests | `RuleEngineTest`, `ScamAnalyzerServiceTest` (7 required scenarios), `LlmPromptServiceParseTest` |

## Safety model — how rule engine + LLM combine
1. Rule engine detects modular signals (KYC, OTP reveal, UPI/ATM PIN, account-blocked threats, urgency, rewards, payment requests, shortened URLs, abusive TLDs, brand-lookalike domains) and computes a **preliminary risk**.
2. LLM produces the elderly-first explanation (strict JSON, `response_format=json_object`).
3. **Final risk = MAX(rule risk, LLM risk).** The LLM can NEVER lower a strong rule signal — e.g. a KYC+OTP message stays HIGH even if the model says LOW. The LLM may only raise risk.
4. If the LLM is unconfigured, times out, or returns invalid JSON → the rule-engine verdict (with bilingual EN/Hinglish guidance) is returned as-is.

API key comes only from `OPENAI_API_KEY` (env var) — never hardcoded.

## Run & test
```bash
cd backend
./mvnw test            # unit tests incl. the 7 required scenarios
./mvnw spring-boot:run # defaults to in-memory H2; set OPENAI_API_KEY for live LLM
```

# Step 1 reference — Spring Boot Backend architecture
| Layer | Files |
|---|---|
| Bootstrap | `SurakshaSaathiApplication.java`, `pom.xml`, `application.yml` |
| Config | `config/CorsConfig.java`, `config/GlobalExceptionHandler.java` |
| DTOs | `ApiResponse`, `AnalysisResponse`, `RiskLevel`, `LlmVerdict`, `TextAnalyzeRequest`, `TrustedAlertRequest/Response`, `EmergencyGuidelineResponse` |
| Models | `TrustedContact`, `AlertLog` (privacy-safe: content never stored) |
| Repository | `TrustedContactRepository`, `AlertLogRepository` |
| Services | Interfaces + **placeholder impls** (real logic in Step 2) |
| Controllers | `AnalyzeController`, `TrustedCircleController`, `EmergencyController` |

## API contract (all responses use the `ApiResponse` envelope)

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/v1/analyze/text` | `{ "text": "...", "language": "en\|hi" }` → structured verdict |
| POST | `/api/v1/analyze/image` | multipart `file` + `language` → OCR → same verdict |
| POST | `/api/v1/trusted-circle/alert` | mock notification to a trusted contact |
| GET  | `/api/v1/emergency/guideline?language=en\|hi` | 1930 / cybercrime.gov.in guidance |

### Structured analysis response
```json
{
  "success": true,
  "message": "Analysis complete",
  "data": {
    "riskLevel": "HIGH",
    "riskLabel": "High Risk",
    "riskEmoji": "🔴",
    "simpleExplanation": "...",
    "safeActions": ["..."],
    "prohibitedActions": ["..."],
    "verificationSteps": "...",
    "matchedSignals": ["KYC", "OTP"],
    "analysisSource": "RULE_ENGINE + LLM",
    "inputType": "TEXT",
    "analyzedAt": "2026-10-08T12:00:00Z"
  }
}
```

## Run it
```bash
cd backend
./mvnw spring-boot:run     # or: mvn spring-boot:run
```
Defaults to in-memory H2 so no MySQL setup is needed for the prototype.
Set `OPENAI_API_KEY` env var — Step 2 switches `app.llm.enabled=true` to live LLM mode.

## Next
**Step 2** — `ScamAnalyzerService` (regex/keyword rule engine + URL checks),
`LlmPromptService` (strict JSON system prompt), real Tess4J OCR.
