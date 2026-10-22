package com.surakshasaathi.ai.dto.trusted;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/v1/trusted-circle/alert
 * Simulates notifying a family member / trusted contact about a suspicious message.
 */
public record TrustedAlertRequest(
        @NotBlank(message = "Please choose who to contact.")
        String contactName,

        /** Phone number of the trusted person (kept only in memory for the prototype). */
        @NotBlank(message = "Please provide the trusted person's phone number.")
        String contactPhone,

        /** The suspicious content the elderly user wants a second opinion on. */
        @Size(max = 5000)
        String suspiciousContent,

        /** Risk level of the analysis that triggered the alert (HIGH / MEDIUM / LOW). */
        String riskLevel,

        /** "en" or "hi" */
        String language
) { }
