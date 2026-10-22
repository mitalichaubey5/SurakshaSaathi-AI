package com.surakshasaathi.ai.service.rule;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A single modular detection rule.
 *
 * Implementations are cheap to add:
 * - simple keyword/pattern rules: use {@link #of(String, ScamSignal, String)}
 * - rules needing custom logic (URL parsing): implement this interface.
 */
public interface ScamRule {

    /** Stable unique id, used to de-duplicate signals. */
    String id();

    /** Run the rule against normalized (lower-cased) text. */
    Optional<ScamSignal> match(String normalizedText);

    /** Convenience factory for regex-based rules with a pre-compiled, case-insensitive pattern. */
    static ScamRule of(String id, ScamSignal signal, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        return new ScamRule() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public Optional<ScamSignal> match(String text) {
                return pattern.matcher(text).find() ? Optional.of(signal) : Optional.empty();
            }
        };
    }
}