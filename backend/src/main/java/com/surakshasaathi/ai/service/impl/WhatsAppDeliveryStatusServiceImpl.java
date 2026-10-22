package com.surakshasaathi.ai.service.impl;

import com.surakshasaathi.ai.model.AlertLog;
import com.surakshasaathi.ai.repository.AlertLogRepository;
import com.surakshasaathi.ai.service.whatsapp.DeliveryStatus;
import com.surakshasaathi.ai.service.whatsapp.WhatsAppDeliveryStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Persists delivery-status transitions for outbound WhatsApp alerts.
 *
 * <p>Security/privacy: only the status name and the numeric provider error code
 * are stored. Message contents, recipient numbers, and raw provider bodies are
 * never persisted or logged; the wamid itself is not logged.</p>
 */
@Service
public class WhatsAppDeliveryStatusServiceImpl implements WhatsAppDeliveryStatusService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppDeliveryStatusServiceImpl.class);

    private final AlertLogRepository alertLogRepository;

    public WhatsAppDeliveryStatusServiceImpl(AlertLogRepository alertLogRepository) {
        this.alertLogRepository = alertLogRepository;
    }

    @Override
    public boolean applyStatus(String providerMessageId, String metaStatus,
                               java.time.Instant occurredAt, String providerErrorCode) {
        if (providerMessageId == null || providerMessageId.isBlank()) {
            return false;
        }
        DeliveryStatus incoming = DeliveryStatus.fromMetaStatus(metaStatus);
        if (incoming == null) {
            // Unknown/unsupported statuses are ignored rather than persisted.
            return false;
        }

        var found = alertLogRepository.findByProviderMessageId(providerMessageId);
        if (found.isEmpty()) {
            // No matching alert (e.g. a message not sent by this instance) - ignore quietly.
            return false;
        }

        var alert = found.get();
        DeliveryStatus current = DeliveryStatus.parse(alert.getDeliveryStatus());
        if (!incoming.shouldApplyAfter(current)) {
            // Duplicate, stale, or out-of-order event: no-op.
            return false;
        }

        alert.updateDeliveryStatus(incoming.name(), providerErrorCode,
                occurredAt == null ? java.time.Instant.now() : occurredAt);
        alertLogRepository.save(alert);
        log.info("WhatsApp delivery status updated: status={}, hasErrorCode={}",
                incoming, providerErrorCode != null && !providerErrorCode.isBlank());
        return true;
    }
}
