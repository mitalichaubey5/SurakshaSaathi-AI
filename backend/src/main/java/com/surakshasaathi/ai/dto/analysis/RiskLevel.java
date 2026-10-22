package com.surakshasaathi.ai.dto.analysis;

/**
 * Risk levels returned by the analysis engine.
 * Maps directly to the UI colour coding:
 *   LOW    -> 🟢 Low Risk
 *   MEDIUM -> 🟡 Needs Verification
 *   HIGH   -> 🔴 High Risk
 */
public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}
