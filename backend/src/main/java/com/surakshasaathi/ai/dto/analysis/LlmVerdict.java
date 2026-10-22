package com.surakshasaathi.ai.dto.analysis;

import java.util.List;

/**
 * The strict JSON contract enforced on the LLM (System Prompt output).
 *
 * The LLM must return EXACTLY this shape — parsed by {@link LlmVerdict} in Step 2:
 * {
 *   "riskLevel": "HIGH | MEDIUM | LOW",
 *   "simpleExplanation": "...",
 *   "safeActions": ["...", "..."],
 *   "prohibitedActions": ["...", "..."],
 *   "verificationSteps": "..."
 * }
 */
public record LlmVerdict(
        RiskLevel riskLevel,
        String simpleExplanation,
        List<String> safeActions,
        List<String> prohibitedActions,
        String verificationSteps
) { }
