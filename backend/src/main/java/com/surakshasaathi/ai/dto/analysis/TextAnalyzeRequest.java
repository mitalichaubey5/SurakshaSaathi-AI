package com.surakshasaathi.ai.dto.analysis;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/v1/analyze/text
 *
 * Privacy-first: the text is analysed in-memory and never persisted.
 */
public record TextAnalyzeRequest(
        @NotBlank(message = "Please paste the suspicious message or link to check.")
        @Size(max = 5000, message = "Message is too long. Please paste only the suspicious part.")
        String text,

        /** "en" (default) or "hi" — controls the language of the simple explanation. */
        String language
) {
    public String safeLanguage() {
        return (language == null || language.isBlank()) ? "en" : language.trim().toLowerCase();
    }
}
