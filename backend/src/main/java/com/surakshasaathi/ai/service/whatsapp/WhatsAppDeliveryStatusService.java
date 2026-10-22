package com.surakshasaathi.ai.service.whatsapp;

import java.time.Instant;

/**
 * Applies verified Meta delivery-status webhooks to persisted alert records.
 *
 * <p>Only events carrying a {@code providerMessageId} we previously stored are
 * considered; unknown ids are ignored. Updates are forward-only and therefore
 * idempotent for duplicate and out-of-order events.</p>
 */
public interface WhatsAppDeliveryStatusService {

    /**
     * @param providerMessageId the Meta message id (wamid) from the webhook
     * @param metaStatus        Meta's status string ("sent", "delivered", "read", "failed")
     * @param occurredAt        event timestamp (nullable; defaults to now)
     * @param providerErrorCode optional Meta numeric error code (failed events only)
     * @return true when a persisted row was actually updated
     */
    boolean applyStatus(String providerMessageId, String metaStatus, Instant occurredAt, String providerErrorCode);
}
