package com.surakshasaathi.ai.service.impl;

import com.surakshasaathi.ai.config.WhatsAppProperties;
import com.surakshasaathi.ai.dto.trusted.TrustedAlertRequest;
import com.surakshasaathi.ai.dto.trusted.TrustedAlertResponse;
import com.surakshasaathi.ai.model.AlertLog;
import com.surakshasaathi.ai.repository.AlertLogRepository;
import com.surakshasaathi.ai.service.TrustedCircleService;
import com.surakshasaathi.ai.service.whatsapp.DeliveryStatus;
import com.surakshasaathi.ai.service.whatsapp.SendResult;
import com.surakshasaathi.ai.service.whatsapp.TemplateMessage;
import com.surakshasaathi.ai.service.whatsapp.WhatsAppCloudClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Sends trusted-circle alerts.
 *
 * <p>When real sending is not enabled - which is the default, and also the case
 * whenever any required setting is missing - this behaves exactly like the
 * original mock notification. When real sending is fully enabled it sends only
 * to the server-configured, allow-listed test recipient, records the resulting
 * delivery metadata, and never claims delivery from a mere provider acceptance.</p>
 */
@Service
public class TrustedCircleServiceImpl implements TrustedCircleService {

    private static final Logger log = LoggerFactory.getLogger(TrustedCircleServiceImpl.class);

    /**
     * Strict E.164-style recipient format: a leading '+' followed by 8-15 digits,
     * the first of which is non-zero. Spaces, letters, other punctuation, and
     * out-of-range lengths are rejected; values are never trimmed or normalized.
     */
    private static final Pattern E164_RECIPIENT = Pattern.compile("^\\+[1-9][0-9]{7,14}$");

    private final AlertLogRepository alertLogRepository;
    private final WhatsAppProperties whatsAppProperties;
    private final WhatsAppCloudClient whatsAppCloudClient;

    public TrustedCircleServiceImpl(AlertLogRepository alertLogRepository,
                                    WhatsAppProperties whatsAppProperties,
                                    WhatsAppCloudClient whatsAppCloudClient) {
        this.alertLogRepository = alertLogRepository;
        this.whatsAppProperties = whatsAppProperties;
        this.whatsAppCloudClient = whatsAppCloudClient;
    }

    @Override
    public TrustedAlertResponse sendAlert(TrustedAlertRequest request) {
        String risk = request.riskLevel() == null ? "UNKNOWN" : request.riskLevel().toUpperCase(Locale.ROOT);
        String lang = request.language() == null ? "en" : request.language();

        if (!isRealSendingEnabled()) {
            return sendMock(request, risk, lang);
        }
        return sendViaWhatsApp(request, risk, lang);
    }

    /**
     * Real sending requires both independent flags plus usable credentials.
     * Anything missing falls back to the original mock behavior, so the
     * prototype never breaks and nothing is ever sent by accident.
     */
    private boolean isRealSendingEnabled() {
        return whatsAppProperties.isEnabled()
                && whatsAppProperties.isSendingEnabled()
                && hasText(whatsAppProperties.getAccessToken())
                && hasText(whatsAppProperties.getPhoneNumberId());
    }

    private TrustedAlertResponse sendMock(TrustedAlertRequest request, String risk, String lang) {
        alertLogRepository.save(new AlertLog(request.contactName(), risk, lang));

        boolean hindi = "hi".equalsIgnoreCase(lang);
        String message = hindi
                ? request.contactName() + " ji ko suchna bhej di gayi hai. Wo jald aapse sampark karenge."
                : "We have sent an alert to " + request.contactName() + ". They can contact you soon.";

        return new TrustedAlertResponse(
                true,
                message,
                "MOCK_SMS + MOCK_PUSH",
                UUID.randomUUID().toString(),
                Instant.now(),
                DeliveryStatus.MOCK.name(),
                null
        );
    }

    private TrustedAlertResponse sendViaWhatsApp(TrustedAlertRequest request, String risk, String lang) {
        // The request-supplied phone number is intentionally ignored for real sends:
        // messages only ever go to the server-configured, allow-listed test recipient.
        String recipient = resolveConfiguredRecipient();
        if (recipient == null) {
            AlertLog rejected = new AlertLog(request.contactName(), risk, lang);
            rejected.setChannel("WHATSAPP");
            rejected.updateDeliveryStatus(DeliveryStatus.REJECTED_RECIPIENT.name(), null, Instant.now());
            rejected = alertLogRepository.save(rejected);
            log.warn("WhatsApp send rejected: no allow-listed test recipient configured");
            return realResponse(DeliveryStatus.REJECTED_RECIPIENT, rejected, null, lang);
        }

        AlertLog alert = new AlertLog(request.contactName(), risk, lang);
        alert.setChannel("WHATSAPP");
        alert = alertLogRepository.save(alert);

        SendResult result = whatsAppCloudClient.sendTemplate(new TemplateMessage(
                recipient,
                whatsAppProperties.getTemplateName(),
                whatsAppProperties.getTemplateLanguage()));

        DeliveryStatus status;
        String providerMessageId = null;
        String errorCode = null;
        switch (result.outcome()) {
            case ACCEPTED -> {
                status = DeliveryStatus.ACCEPTED;
                providerMessageId = result.providerMessageId();
            }
            case TIMEOUT -> status = DeliveryStatus.UNKNOWN;
            case FAILED -> {
                status = DeliveryStatus.FAILED;
                errorCode = result.errorCode();
            }
            default -> status = DeliveryStatus.FAILED;
        }

        alert.setProviderMessageId(providerMessageId);
        alert.updateDeliveryStatus(status.name(), errorCode, Instant.now());
        alert = alertLogRepository.save(alert);

        // Log the status only - never the recipient, wamid, or message contents.
        log.info("WhatsApp send attempt recorded: status={}", status);
        return realResponse(status, alert, providerMessageId, lang);
    }

    private TrustedAlertResponse realResponse(DeliveryStatus status, AlertLog alert,
                                              String providerMessageId, String lang) {
        boolean hindi = "hi".equalsIgnoreCase(lang);
        String message = switch (status) {
            case ACCEPTED -> hindi
                    ? "Test WhatsApp sandesh provider ne sweekar kiya. Delivery abhi confirm nahi hai."
                    : "The test WhatsApp message was accepted by the provider. Delivery is not confirmed yet.";
            case UNKNOWN -> hindi
                    ? "WhatsApp anurodh ka samay samapt ho gaya. Delivery confirm nahi hai."
                    : "The WhatsApp request timed out. Delivery is not confirmed.";
            case REJECTED_RECIPIENT -> hindi
                    ? "Koi verified test recipient set nahi hai; koi sandesh nahi bheja gaya."
                    : "No verified test recipient is configured; no message was sent.";
            default -> hindi
                    ? "WhatsApp sandesh nahi bheja ja saka."
                    : "The WhatsApp message could not be sent.";
        };
        // delivered is only ever true for the mock path; ACCEPTED is not delivery.
        return new TrustedAlertResponse(
                false,
                message,
                "WHATSAPP",
                alert.getId() == null ? null : String.valueOf(alert.getId()),
                Instant.now(),
                status.name(),
                providerMessageId
        );
    }

    /**
     * Resolves the single server-configured, allow-listed test recipient.
     *
     * <p>The request-supplied phone number is never consulted. Each configured
     * value is validated strictly as an E.164-style number (see
     * {@link #isValidTestRecipient(String)}); invalid values are rejected rather
     * than normalized, and the caller falls back to
     * {@link DeliveryStatus#REJECTED_RECIPIENT} without any outbound call.</p>
     *
     * @return the first valid configured recipient, or null when none is valid
     */
    private String resolveConfiguredRecipient() {
        Set<String> configured = new LinkedHashSet<>();
        addRecipients(configured, whatsAppProperties.getTestRecipient());
        addRecipients(configured, whatsAppProperties.getTestRecipients());
        for (String candidate : configured) {
            if (isValidTestRecipient(candidate)) {
                return candidate;
            }
            // Never log the candidate value itself (it may be a phone number).
            log.warn("Ignoring configured WhatsApp test recipient with invalid E.164 format");
        }
        return null;
    }

    /**
     * Strict E.164-style check: a leading '+' followed by 8-15 digits, the first
     * of which is not zero. Rejects spaces, letters, other punctuation, and
     * malformed values; performs no trimming or normalization.
     */
    static boolean isValidTestRecipient(String value) {
        return value != null && E164_RECIPIENT.matcher(value).matches();
    }

    private static void addRecipients(Set<String> target, String raw) {
        if (raw == null) {
            return;
        }
        for (String part : raw.split(",", -1)) {
            target.add(part);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
