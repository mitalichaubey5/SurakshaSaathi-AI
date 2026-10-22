package com.surakshasaathi.ai.service;

import com.surakshasaathi.ai.dto.analysis.LlmVerdict;

import java.util.List;
import java.util.Optional;

/**
 * LLM prompt contract.
 *
 * Builds the structured System Prompt ("Pause. Verify. Then Trust.") that forces
 * the model to return the strict JSON verdict (riskLevel, simpleExplanation,
 * safeActions, prohibitedActions, verificationSteps).
 *
 * Returns {@link Optional#empty()} when the LLM is not configured, times out,
 * returns invalid JSON, or the provider errors — the caller must then fall
 * back to the rule-engine result.
 */
public interface LlmPromptService {

    Optional<LlmVerdict> getVerdict(String suspiciousText, List<String> ruleEngineSignals, String language);
}