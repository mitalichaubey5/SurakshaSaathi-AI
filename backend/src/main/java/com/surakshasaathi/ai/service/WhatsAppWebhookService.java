package com.surakshasaathi.ai.service;

/**
 * Handles a verified WhatsApp Cloud API webhook notification.
 *
 * Phase 1 acknowledges and counts events only — no persistence, no outbound
 * calls, and no message contents are logged. Delivery-status correlation is a
 * later phase.
 */
public interface WhatsAppWebhookService {

    /**
     * Processes an already signature-verified notification payload.
     *
     * @param rawBody the exact raw request bytes received from Meta
     */
    void handleNotification(byte[] rawBody);
}
