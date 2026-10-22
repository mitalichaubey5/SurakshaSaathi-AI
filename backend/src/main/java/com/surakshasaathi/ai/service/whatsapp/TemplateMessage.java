package com.surakshasaathi.ai.service.whatsapp;

/**
 * A WhatsApp template message request. Phase 2 uses Meta's {@code hello_world}
 * template, which takes no parameters, so no personal data is placed in the
 * message body.
 */
public record TemplateMessage(String toE164, String templateName, String languageCode) {
}
