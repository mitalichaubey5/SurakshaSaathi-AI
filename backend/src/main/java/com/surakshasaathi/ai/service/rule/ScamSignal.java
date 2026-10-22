package com.surakshasaathi.ai.service.rule;

/**
 * A single detected scam signal.
 *
 * @param code     stable identifier (used in tests and for de-duplication)
 * @param label    human-readable label shown to the user (e.g. "KYC update demanded")
 * @param severity 1 = weak hint, 2 = moderate, 3 = strong scam signal
 */
public record ScamSignal(String code, String label, int severity) {

    public ScamSignal {
        if (severity < 1 || severity > 3) {
            throw new IllegalArgumentException("severity must be between 1 and 3");
        }
    }
}