package com.surakshasaathi.ai.service.rule;

import com.surakshasaathi.ai.dto.analysis.RiskLevel;

import java.util.List;

/**
 * Output of {@link RuleEngine#detect(String)}: detected signals plus the
 * preliminary (rule-only) risk level.
 */
public record RuleResult(List<ScamSignal> signals, RiskLevel preliminaryRisk) {

    public List<String> signalCodes() {
        return signals.stream().map(ScamSignal::code).toList();
    }

    public List<String> signalLabels() {
        return signals.stream().map(ScamSignal::label).toList();
    }
}