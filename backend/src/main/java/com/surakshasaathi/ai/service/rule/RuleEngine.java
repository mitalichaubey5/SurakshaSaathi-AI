package com.surakshasaathi.ai.service.rule;

import com.surakshasaathi.ai.dto.analysis.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Rule-based heuristic scam detection engine.
 *
 * Runs every rule in the {@link ScamRules} catalog against normalized text,
 * de-duplicates signals, and computes a conservative preliminary risk.
 *
 * Scoring:
 *   - any severity-3 signal (e.g. OTP reveal, account-blocked threats) -> HIGH
 *   - two or more moderate (severity-2) signals -> HIGH
 *   - one moderate signal -> MEDIUM
 *   - two or more weak hints -> MEDIUM
 *   - otherwise LOW
 *
 * "When in doubt, be cautious" — an elderly-first choice: safe by default.
 */
@Component
public class RuleEngine {

    private final List<ScamRule> rules;

    public RuleEngine() {
        this(ScamRules.catalog());
    }

    public RuleEngine(List<ScamRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public RuleResult detect(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Please paste the suspicious message or link to check.");
        }
        String normalized = text.toLowerCase(Locale.ROOT);

        List<ScamSignal> signals = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ScamRule rule : rules) {
            rule.match(normalized).ifPresent(signal -> {
                if (seen.add(signal.code())) {
                    signals.add(signal);
                }
            });
        }
        return new RuleResult(signals, preliminaryRisk(signals));
    }

    static RiskLevel preliminaryRisk(List<ScamSignal> signals) {
        int maxSeverity = 0;
        int score = 0;
        for (ScamSignal s : signals) {
            maxSeverity = Math.max(maxSeverity, s.severity());
            score += s.severity();
        }
        if (maxSeverity >= 3) {
            return RiskLevel.HIGH;
        }
        if (signals.size() >= 2) {
            return RiskLevel.HIGH; // two moderate signals together are suspicious
        }
        if (maxSeverity >= 2) {
            return RiskLevel.MEDIUM;
        }
        if (score > 1) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }
}