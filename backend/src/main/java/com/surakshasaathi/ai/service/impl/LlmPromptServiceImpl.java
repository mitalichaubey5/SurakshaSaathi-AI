package com.surakshasaathi.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.surakshasaathi.ai.config.LlmProperties;
import com.surakshasaathi.ai.dto.analysis.LlmVerdict;
import com.surakshasaathi.ai.service.LlmPromptService;
import com.surakshasaathi.ai.service.llm.LlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * LLM-backed verdict service (OpenAI-compatible chat completions).
 *
 * Design guarantees:
 *  - API key is read ONLY from configuration/environment (`OPENAI_API_KEY`);
 *    never hardcoded.
 *  - Strict JSON output is enforced via the system prompt + OpenAI's
 *    `response_format: json_object` when supported.
 *  - Timeout, invalid JSON, empty response and provider errors are all caught
 *    and turned into {@link Optional#empty()} so the caller can fall back to
 *    the rule engine — the user always gets an answer.
 */
@Service
public class LlmPromptServiceImpl implements LlmPromptService {

    private static final Logger log = LoggerFactory.getLogger(LlmPromptServiceImpl.class);

    private static final Pattern JSON_FENCE = Pattern.compile("^\\s*```(?:json)?\\s*|```\\s*$", Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;
    private final LlmClient llmClient;
    private final LlmProperties llmProperties;

    public LlmPromptServiceImpl(ObjectMapper objectMapper, LlmClient llmClient, LlmProperties llmProperties) {
        this.objectMapper = objectMapper;
        this.llmClient = llmClient;
        this.llmProperties = llmProperties;
    }

    @Override
    public Optional<LlmVerdict> getVerdict(String suspiciousText, List<String> ruleEngineSignals, String language) {
        if (!llmProperties.isEnabled()) {
            log.warn("LLM is disabled (app.llm.enabled=false). Using rule engine only.");
            return Optional.empty();
        }
        if (!StringUtils.hasText(llmProperties.getApiKey())) {
            log.warn("LLM API key not configured (set OPENAI_API_KEY). Using rule engine only.");
            return Optional.empty();
        }
        try {
            String content = callChatCompletions(suspiciousText, ruleEngineSignals, language);
            LlmVerdict verdict = parseJson(content, objectMapper);
            if (verdict == null) {
                log.warn("LLM returned empty or invalid JSON; falling back to rule engine.");
                return Optional.empty();
            }
            return Optional.of(normalize(verdict));
        } catch (Exception ex) {
            // Covers timeouts (SocketTimeoutException), HTTP errors, and any parse failure.
            log.warn("LLM call failed ({}); falling back to rule engine.", ex.getMessage());
            return Optional.empty();
        }
    }

    private String callChatCompletions(String suspiciousText, List<String> ruleEngineSignals, String language) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", llmProperties.getModel());
        body.put("temperature", 0.2);
        body.put("messages", List.of(
                Map.of("role", "system", "content", buildSystemPrompt(language)),
                Map.of("role", "user", "content", buildUserPrompt(suspiciousText, ruleEngineSignals))
        ));
        // Supported by OpenAI and most compatible providers; harmless if ignored.
        body.put("response_format", Map.of("type", "json_object"));

        return llmClient.chatCompletions(body);
    }

    /**
     * Parses LLM content into {@link LlmVerdict}, tolerating markdown fences and
     * surrounding prose. Returns null when content is empty or invalid JSON.
     */
    static LlmVerdict parseJson(String content, ObjectMapper baseMapper) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        ObjectMapper mapper = baseMapper.copy()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);

        String cleaned = JSON_FENCE.matcher(content).replaceAll("").trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        try {
            LlmVerdict verdict = mapper.readValue(cleaned.substring(start, end + 1), LlmVerdict.class);
            if (verdict.riskLevel() == null || !StringUtils.hasText(verdict.simpleExplanation())) {
                return null; // required fields missing
            }
            return verdict;
        } catch (JsonProcessingException | RuntimeException ex) {
            return null;
        }
    }

    private static LlmVerdict normalize(LlmVerdict v) {
        return new LlmVerdict(
                v.riskLevel(),
                v.simpleExplanation().trim(),
                cleanList(v.safeActions()),
                cleanList(v.prohibitedActions()),
                StringUtils.hasText(v.verificationSteps()) ? v.verificationSteps().trim() : "");
    }

    private static List<String> cleanList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<String> cleaned = new ArrayList<>();
        for (String item : items) {
            if (StringUtils.hasText(item)) {
                cleaned.add(item.trim());
            }
        }
        return cleaned;
    }

    /** Elderly-first system prompt with a strict JSON contract. */
    private static String buildSystemPrompt(String language) {
        boolean hindi = "hi".equalsIgnoreCase(language);
        String languageRule = hindi
                ? "WRITE simpleExplanation, safeActions, prohibitedActions and verificationSteps IN SIMPLE HINDI using Roman script (Hinglish) so an elderly Indian can understand easily. Keep \"riskLevel\" and all JSON field names in English."
                : "WRITE simpleExplanation, safeActions, prohibitedActions and verificationSteps IN SIMPLE ENGLISH: short words, short sentences, no technical jargon.";

        return """
                You are SurakshaSaathi, a caring safety helper for elderly citizens in India.
                Motto: "Pause. Verify. Then Trust." Core flow: Detect, Explain, Verify, Guide, Escalate.

                You receive a suspicious message or URL. Help exactly like a calm, trustworthy
                grandson or granddaughter would: kind, patient, and very clear.

                STRICT OUTPUT RULES:
                1. Return ONLY valid JSON. No markdown, no code fences, no words before or after it.
                2. Use EXACTLY this structure:
                   {
                     "riskLevel": "HIGH|MEDIUM|LOW",
                     "simpleExplanation": "2-3 easy sentences",
                     "safeActions": ["step 1", "step 2"],
                     "prohibitedActions": ["step 1", "step 2"],
                     "verificationSteps": "1-2 easy sentences"
                   }
                3. riskLevel: HIGH = clear scam signs; MEDIUM = unclear, needs a trusted human to check; LOW = normal harmless message.
                4. simpleExplanation: plain words, NO jargon (never say "phishing", "credential", "heuristic"). Say what is wrong and why, kindly.
                5. safeActions: 1-3 very simple next steps.
                6. prohibitedActions: 1-3 steps starting with "Do not". ALWAYS include: never click links from the message, never share OTP / UPI PIN / ATM PIN / MPIN / password / CVV / card number.
                7. verificationSteps: how to confirm through OFFICIAL channels only (the official app, the official website typed by the user themselves, the official helpline). NEVER suggest using any number or link written inside the suspicious message.
                8. %s

                NON-NEGOTIABLE SAFETY: never instruct the user to click a link, call a number, or share any sensitive code that appears inside the suspicious message.
                """.formatted(languageRule);
    }

    private static String buildUserPrompt(String suspiciousText, List<String> ruleEngineSignals) {
        String signals = (ruleEngineSignals == null || ruleEngineSignals.isEmpty())
                ? "none detected"
                : String.join(", ", ruleEngineSignals);
        return "Detected by our safety rules (context to confirm, do not repeat mechanically): " + signals
                + "\n\nSUSPICIOUS MESSAGE TO REVIEW:\n" + suspiciousText;
    }
}