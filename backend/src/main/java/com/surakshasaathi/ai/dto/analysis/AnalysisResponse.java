package com.surakshasaathi.ai.dto.analysis;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

/**
 * FINAL structured response returned to the frontend for every analysis.
 *
 * Endpoint contract (both /analyze/text and /analyze/image):
 * {
 *   "success": true,
 *   "message": "Analysis complete",
 *   "data": {
 *     "riskLevel": "HIGH",
 *     "riskLabel": "High Risk",
 *     "riskEmoji": "🔴",
 *     "simpleExplanation": "...",
 *     "safeActions": ["..."],
 *     "prohibitedActions": ["..."],
 *     "verificationSteps": "...",
 *     "matchedSignals": ["KYC", "OTP"],
 *     "analysisSource": "RULE_ENGINE + LLM",
 *     "inputType": "TEXT | IMAGE",
 *     "ocrText": "..."            // only for IMAGE inputs
 *     "analyzedAt": "2026-10-08T..."
 *   }
 * }
 */
public record AnalysisResponse(
        RiskLevel riskLevel,
        String simpleExplanation,
        List<String> safeActions,
        List<String> prohibitedActions,
        String verificationSteps,
        List<String> matchedSignals,
        String analysisSource,
        String inputType,
        String ocrText,
        Instant analyzedAt
) {

    /** UI-ready label: LOW -> "Low Risk", MEDIUM -> "Needs Verification", HIGH -> "High Risk". */
    @JsonProperty("riskLabel")
    public String riskLabel() {
        return switch (riskLevel) {
            case LOW -> "Low Risk";
            case MEDIUM -> "Needs Verification";
            case HIGH -> "High Risk";
        };
    }

    /** UI-ready emoji: 🟢 | 🟡 | 🔴 */
    @JsonProperty("riskEmoji")
    public String riskEmoji() {
        return switch (riskLevel) {
            case LOW -> "🟢";
            case MEDIUM -> "🟡";
            case HIGH -> "🔴";
        };
    }
}
