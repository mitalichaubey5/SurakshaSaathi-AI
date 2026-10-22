package com.surakshasaathi.ai.service.impl;

import com.surakshasaathi.ai.model.AlertLog;
import com.surakshasaathi.ai.repository.AlertLogRepository;
import com.surakshasaathi.ai.service.whatsapp.DeliveryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for applying verified Meta delivery-status events to stored alerts:
 * successful update, duplicate, out-of-order, unknown id, and unsupported status.
 */
class WhatsAppDeliveryStatusServiceImplTest {

    private static final String WAMID = "wamid.ABC123";

    private AlertLogRepository repository;
    private WhatsAppDeliveryStatusServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(AlertLogRepository.class);
        service = new WhatsAppDeliveryStatusServiceImpl(repository);
    }

    private AlertLog alertWithStatus(DeliveryStatus status) {
        AlertLog alert = new AlertLog("Ravi", "HIGH", "en");
        alert.setChannel("WHATSAPP");
        alert.setProviderMessageId(WAMID);
        alert.updateDeliveryStatus(status.name(), null, Instant.now());
        return alert;
    }

    @Test
    void appliesForwardStatusUpdate() {
        AlertLog alert = alertWithStatus(DeliveryStatus.ACCEPTED);
        when(repository.findByProviderMessageId(WAMID)).thenReturn(Optional.of(alert));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Instant eventTime = Instant.ofEpochSecond(1_700_000_000L);

        boolean updated = service.applyStatus(WAMID, "delivered", eventTime, null);

        assertThat(updated).isTrue();
        assertThat(alert.getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED.name());
        assertThat(alert.getStatusUpdatedAt()).isEqualTo(eventTime);
        verify(repository).save(alert);
    }

    @Test
    void failedStatusStoresProviderErrorCodeOnly() {
        AlertLog alert = alertWithStatus(DeliveryStatus.SENT);
        when(repository.findByProviderMessageId(WAMID)).thenReturn(Optional.of(alert));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean updated = service.applyStatus(WAMID, "failed", Instant.now(), "131047");

        assertThat(updated).isTrue();
        assertThat(alert.getDeliveryStatus()).isEqualTo(DeliveryStatus.FAILED.name());
        assertThat(alert.getProviderErrorCode()).isEqualTo("131047");
    }

    @Test
    void duplicateDeliveredEventIsANoOp() {
        AlertLog alert = alertWithStatus(DeliveryStatus.DELIVERED);
        when(repository.findByProviderMessageId(WAMID)).thenReturn(Optional.of(alert));

        boolean updated = service.applyStatus(WAMID, "delivered", Instant.now(), null);

        assertThat(updated).isFalse();
        verify(repository, never()).save(any());
    }

    @Test
    void outOfOrderSentEventAfterDeliveredIsANoOp() {
        AlertLog alert = alertWithStatus(DeliveryStatus.DELIVERED);
        when(repository.findByProviderMessageId(WAMID)).thenReturn(Optional.of(alert));

        boolean updated = service.applyStatus(WAMID, "sent", Instant.now(), null);

        assertThat(updated).isFalse();
        verify(repository, never()).save(any());
    }

    @Test
    void failedCannotDowngradeADeliveredMessage() {
        AlertLog alert = alertWithStatus(DeliveryStatus.DELIVERED);
        when(repository.findByProviderMessageId(WAMID)).thenReturn(Optional.of(alert));

        boolean updated = service.applyStatus(WAMID, "failed", Instant.now(), "131047");

        assertThat(updated).isFalse();
        verify(repository, never()).save(any());
    }

    @Test
    void unknownMessageIdIsIgnored() {
        when(repository.findByProviderMessageId("wamid.UNKNOWN")).thenReturn(Optional.empty());

        boolean updated = service.applyStatus("wamid.UNKNOWN", "delivered", Instant.now(), null);

        assertThat(updated).isFalse();
        verify(repository, never()).save(any());
    }

    @Test
    void unsupportedMetaStatusIsIgnored() {
        boolean updated = service.applyStatus(WAMID, "queued", Instant.now(), null);

        assertThat(updated).isFalse();
        verifyNoInteractions(repository);
    }

    @Test
    void blankMessageIdIsIgnored() {
        assertThat(service.applyStatus(null, "delivered", Instant.now(), null)).isFalse();
        assertThat(service.applyStatus("  ", "delivered", Instant.now(), null)).isFalse();
        verifyNoInteractions(repository);
    }

    @Test
    void missingTimestampDefaultsToNow() {
        AlertLog alert = alertWithStatus(DeliveryStatus.ACCEPTED);
        when(repository.findByProviderMessageId(WAMID)).thenReturn(Optional.of(alert));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Instant before = Instant.now();

        boolean updated = service.applyStatus(WAMID, "read", null, null);

        assertThat(updated).isTrue();
        assertThat(alert.getStatusUpdatedAt()).isNotNull();
        assertThat(alert.getStatusUpdatedAt()).isAfterOrEqualTo(before);
    }
}
