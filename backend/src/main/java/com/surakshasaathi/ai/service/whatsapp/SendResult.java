package com.surakshasaathi.ai.service.whatsapp;

/**
 * Outcome of a single WhatsApp Cloud API send attempt.
 *
 * {@code errorCode} holds only the Meta numeric error code; {@code errorSummary}
 * is a short, safe description. Raw provider response bodies and message content
 * are never carried here (and never logged).
 */
public record SendResult(Outcome outcome, String providerMessageId, String errorCode, String errorSummary) {

    public enum Outcome {
        /** The API accepted the message and returned a message id (not yet delivered). */
        ACCEPTED,
        /** The provider rejected the message (4xx/5xx with an error body). */
        FAILED,
        /** Timeout or ambiguous network outcome: acceptance is unknown. */
        TIMEOUT,
        /** Sending is not configured/enabled. */
        DISABLED
    }

    public static SendResult accepted(String providerMessageId) {
        return new SendResult(Outcome.ACCEPTED, providerMessageId, null, null);
    }

    public static SendResult failed(String errorCode, String errorSummary) {
        return new SendResult(Outcome.FAILED, null, errorCode, errorSummary);
    }

    public static SendResult timeout() {
        return new SendResult(Outcome.TIMEOUT, null, null, "network timeout");
    }

    public static SendResult disabled() {
        return new SendResult(Outcome.DISABLED, null, null, "sending disabled or not configured");
    }

    public boolean isAccepted() {
        return outcome == Outcome.ACCEPTED;
    }
}
