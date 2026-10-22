package com.surakshasaathi.ai.service.whatsapp;

/**
 * Abstraction over the outbound WhatsApp Cloud API.
 *
 * The real implementation is only wired when {@code app.whatsapp.enabled=true}
 * and therefore never has credentials configured by default.
 */
public interface WhatsAppCloudClient {

    /**
     * Attempts to send a template message.
     *
     * @return a {@link SendResult}; a successful (ACCEPTED) result only means the
     *         provider accepted the request, never that it was delivered
     */
    SendResult sendTemplate(TemplateMessage message);
}
