package com.surakshasaathi.ai.service.whatsapp;

import java.util.Locale;

/**
 * Honest lifecycle of an escalation notification.
 *
 * The key distinction is that {@link #ACCEPTED} only means the Meta Cloud API
 * returned a message id; real delivery is confirmed later by a webhook
 * ({@link #SENT}, {@link #DELIVERED}, {@link #READ}) or {@link #FAILED}.
 */
public enum DeliveryStatus {
    MOCK,
    ACCEPTED,
    SENT,
    DELIVERED,
    READ,
    FAILED,
    /** Timed out / ambiguous network outcome: we do not know if Meta accepted it. */
    UNKNOWN,
    REJECTED_RECIPIENT;

    /** Maps a Meta webhook {@code status} value, or null when unknown. */
    public static DeliveryStatus fromMetaStatus(String metaStatus) {
        if (metaStatus == null || metaStatus.isBlank()) {
            return null;
        }
        return switch (metaStatus.trim().toLowerCase(Locale.ROOT)) {
            case "sent" -> SENT;
            case "delivered" -> DELIVERED;
            case "read" -> READ;
            case "failed" -> FAILED;
            default -> null;
        };
    }

    /** Lenient parse of a stored status name; null when unknown/blank. */
    public static DeliveryStatus parse(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return DeliveryStatus.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private int progressRank() {
        return switch (this) {
            case ACCEPTED -> 1;
            case SENT -> 2;
            case DELIVERED -> 3;
            case READ -> 4;
            default -> 0;
        };
    }

    /**
     * Forward-only rule for webhook updates, so retries and out-of-order events
     * are idempotent:
     *   ACCEPTED -> SENT -> DELIVERED -> READ.
     *   FAILED may replace ACCEPTED/SENT (nothing delivered yet) but never
     *   DELIVERED/READ. Nothing follows FAILED/REJECTED_RECIPIENT/READ.
     *
     * @param current the currently persisted status (may be null)
     * @return true when {@code this} (the incoming status) should be applied
     */
    public boolean shouldApplyAfter(DeliveryStatus current) {
        if (current == null) {
            return true;
        }
        if (this == current) {
            return false;
        }
        if (current == FAILED || current == REJECTED_RECIPIENT || current == READ) {
            return false;
        }
        if (this == FAILED) {
            return current == ACCEPTED || current == SENT;
        }
        if (this == REJECTED_RECIPIENT || this == UNKNOWN || this == MOCK) {
            return false;
        }
        return this.progressRank() > current.progressRank();
    }
}
