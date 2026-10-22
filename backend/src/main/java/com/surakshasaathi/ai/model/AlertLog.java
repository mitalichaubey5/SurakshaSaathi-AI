package com.surakshasaathi.ai.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Minimal audit trail when an alert is sent from the trusted circle.
 *
 * <p>Privacy-first: only the contact name, risk level, and delivery metadata are
 * stored - never the analyzed content, message bodies, tokens, or recipient
 * numbers. Delivery metadata exists so verified Meta webhooks can correlate
 * a message id (wamid) back to the alert and advance its status.</p>
 *
 * <p><strong>Production note:</strong> JPA runs with {@code ddl-auto=update} for
 * the in-memory H2 prototype. A production MySQL deployment must apply a schema
 * migration (add {@code channel}, {@code provider_message_id},
 * {@code delivery_status}, {@code provider_error_code}, {@code status_updated_at})
 * before deploying this version.</p>
 */
@Entity
@Table(name = "alert_logs")
public class AlertLog {

    /** Default channel/status for the legacy mock notification path. */
    public static final String CHANNEL_MOCK = "MOCK";
    public static final String STATUS_MOCK = "MOCK";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contact_name", length = 255)
    private String contactName;

    @Column(name = "risk_level", length = 32)
    private String riskLevel;

    @Column(name = "language", length = 8)
    private String language;

    /** Delivery channel, e.g. {@code MOCK} or {@code WHATSAPP}. */
    @Column(name = "channel", length = 20)
    private String channel = CHANNEL_MOCK;

    /** WhatsApp message id returned by Meta once a send is accepted. */
    @Column(name = "provider_message_id", length = 128)
    private String providerMessageId;

    /** Forward-only delivery status; see {@code DeliveryStatus}. */
    @Column(name = "delivery_status", length = 20)
    private String deliveryStatus = STATUS_MOCK;

    /** Numeric Meta error code for a failed send (never a raw error body). */
    @Column(name = "provider_error_code", length = 20)
    private String providerErrorCode;

    /** When the delivery status last changed (from a verified webhook or send result). */
    @Column(name = "status_updated_at")
    private Instant statusUpdatedAt;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (channel == null) {
            channel = CHANNEL_MOCK;
        }
        if (deliveryStatus == null) {
            deliveryStatus = STATUS_MOCK;
        }
    }

    public AlertLog() {
    }

    public AlertLog(String contactName, String riskLevel, String language) {
        this.contactName = contactName;
        this.riskLevel = riskLevel;
        this.language = language;
    }

    public Long getId() {
        return id;
    }

    public String getContactName() {
        return contactName;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public String getLanguage() {
        return language;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getChannel() {
        return channel;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public String getProviderErrorCode() {
        return providerErrorCode;
    }

    public Instant getStatusUpdatedAt() {
        return statusUpdatedAt;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    /**
     * Records a delivery-status transition. Callers are responsible for
     * enforcing forward-only ordering (see {@code DeliveryStatus.shouldApplyAfter}).
     */
    public void updateDeliveryStatus(String deliveryStatus, String providerErrorCode, Instant updatedAt) {
        this.deliveryStatus = deliveryStatus;
        this.providerErrorCode = providerErrorCode;
        this.statusUpdatedAt = updatedAt;
    }
}
