package com.surakshasaathi.ai.service.whatsapp;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * No-op WhatsApp client used whenever real sending is not enabled.
 *
 * <p>Active by default ({@code app.whatsapp.enabled} missing or false), so the
 * application never constructs an HTTP client or touches the network unless the
 * operator explicitly opts in. It reports {@link SendResult.Outcome#DISABLED}
 * and never sends anything.</p>
 */
@Component
@ConditionalOnProperty(prefix = "app.whatsapp", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledWhatsAppCloudClient implements WhatsAppCloudClient {

    @Override
    public SendResult sendTemplate(TemplateMessage message) {
        return SendResult.disabled();
    }
}
