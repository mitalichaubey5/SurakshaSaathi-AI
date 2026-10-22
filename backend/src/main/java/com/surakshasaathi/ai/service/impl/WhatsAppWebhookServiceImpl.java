package com.surakshasaathi.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.surakshasaathi.ai.service.WhatsAppWebhookService;
import com.surakshasaathi.ai.service.whatsapp.WhatsAppDeliveryStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Webhook handler for WhatsApp Cloud API notifications.
 *
 * <p>Handles two kinds of changes found in a verified payload:</p>
 * <ul>
 *   <li><strong>statuses</strong> - delivery updates, applied to stored alerts via
 *       {@link WhatsAppDeliveryStatusService} (forward-only, idempotent).</li>
 *   <li><strong>messages</strong> - inbound messages, still counted only; message
 *       contents are never logged or persisted.</li>
 * </ul>
 *
 * <p>Only aggregate counts and status names are logged. Raw payloads, tokens,
 * message contents, and recipient numbers are never logged. A malformed event is
 * skipped without failing the whole notification so Meta's retries are not
 * amplified.</p>
 */
@Service
public class WhatsAppWebhookServiceImpl implements WhatsAppWebhookService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookServiceImpl.class);

    private final ObjectMapper objectMapper;
    private final WhatsAppDeliveryStatusService deliveryStatusService;

    public WhatsAppWebhookServiceImpl(ObjectMapper objectMapper,
                                      WhatsAppDeliveryStatusService deliveryStatusService) {
        this.objectMapper = objectMapper;
        this.deliveryStatusService = deliveryStatusService;
    }

    @Override
    public void handleNotification(byte[] rawBody) {
        try {
            int entries = 0;
            int statuses = 0;
            int messages = 0;

            if (rawBody != null && rawBody.length > 0) {
                JsonNode root = objectMapper.readTree(rawBody);
                JsonNode entryNodes = root.path("entry");
                entries = entryNodes.size();
                for (JsonNode entry : entryNodes) {
                    for (JsonNode change : entry.path("changes")) {
                        JsonNode value = change.path("value");

                        JsonNode statusNodes = value.path("statuses");
                        statuses += statusNodes.size();
                        for (JsonNode status : statusNodes) {
                            applyStatus(status);
                        }

                        messages += value.path("messages").size();
                    }
                }
            }

            log.info("WhatsApp webhook received: entries={}, statuses={}, messages={}", entries, statuses, messages);
        } catch (Exception ex) {
            // Valid signature but unparseable body: log the fact only, never the contents,
            // and remain silent to the caller so Meta's retry loop is not amplified.
            log.warn("WhatsApp webhook payload could not be parsed");
        }
    }

    private void applyStatus(JsonNode status) {
        try {
            String providerMessageId = status.path("id").asText(null);
            String metaStatus = status.path("status").asText(null);

            long timestamp = status.path("timestamp").asLong(0L);
            Instant occurredAt = timestamp > 0L ? Instant.ofEpochSecond(timestamp) : Instant.now();

            String errorCode = null;
            JsonNode errors = status.path("errors");
            if (errors.isArray() && !errors.isEmpty()) {
                errorCode = errors.get(0).path("code").asText(null);
            }

            deliveryStatusService.applyStatus(providerMessageId, metaStatus, occurredAt, errorCode);
        } catch (Exception ex) {
            // One malformed status must not discard the rest of the batch.
            log.warn("Skipping malformed WhatsApp status event");
        }
    }
}
