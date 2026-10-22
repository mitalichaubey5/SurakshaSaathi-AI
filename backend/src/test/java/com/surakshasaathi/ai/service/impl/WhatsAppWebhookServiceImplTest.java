package com.surakshasaathi.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surakshasaathi.ai.service.whatsapp.WhatsAppDeliveryStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for the webhook handler: verified delivery-status events are
 * delegated with parsed fields, inbound messages are only counted, and a
 * malformed payload never fails the request (so Meta retries are not amplified).
 */
class WhatsAppWebhookServiceImplTest {

    private WhatsAppDeliveryStatusService deliveryStatusService;
    private WhatsAppWebhookServiceImpl service;

    @BeforeEach
    void setUp() {
        deliveryStatusService = mock(WhatsAppDeliveryStatusService.class);
        service = new WhatsAppWebhookServiceImpl(new com.fasterxml.jackson.databind.ObjectMapper(), deliveryStatusService);
    }

    private byte[] bytes(String body) {
        return body.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void statusEventIsDelegatedWithParsedFields() {
        String body = """
                {"object":"whatsapp_business_account","entry":[{"id":"1","changes":[{"field":"messages",
                "value":{"statuses":[{"id":"wamid.XYZ","status":"delivered","timestamp":1700000000,
                "recipient_id":"15550001111"}]}}]}]}
                """;

        service.handleNotification(bytes(body));

        verify(deliveryStatusService).applyStatus(
                "wamid.XYZ", "delivered", Instant.ofEpochSecond(1_700_000_000L), null);
    }

    @Test
    void failedStatusCarriesProviderErrorCode() {
        String body = """
                {"object":"whatsapp_business_account","entry":[{"changes":[{"field":"messages",
                "value":{"statuses":[{"id":"wamid.ERR","status":"failed","timestamp":1700000001,
                "errors":[{"code":131047,"title":"Re-engagement message"}]}]}}]}]}
                """;

        service.handleNotification(bytes(body));

        verify(deliveryStatusService).applyStatus("wamid.ERR", "failed", Instant.ofEpochSecond(1_700_000_001L), "131047");
    }

    @Test
    void inboundMessagesAreCountedButNotPersisted() {
        String body = """
                {"object":"whatsapp_business_account","entry":[{"changes":[{"field":"messages",
                "value":{"messages":[{"from":"15550001111","id":"wamid.IN","type":"text",
                "text":{"body":"hello there"}}]}}]}]}
                """;

        service.handleNotification(bytes(body));

        verifyNoInteractions(deliveryStatusService);
    }

    @Test
    void multipleStatusEventsAreEachDelegated() {
        String body = """
                {"object":"whatsapp_business_account","entry":[{"changes":[{"field":"messages",
                "value":{"statuses":[{"id":"wamid.A","status":"sent","timestamp":1700000000},
                {"id":"wamid.B","status":"read","timestamp":1700000002}]}}]}]}
                """;

        service.handleNotification(bytes(body));

        verify(deliveryStatusService).applyStatus("wamid.A", "sent", Instant.ofEpochSecond(1_700_000_000L), null);
        verify(deliveryStatusService).applyStatus("wamid.B", "read", Instant.ofEpochSecond(1_700_000_002L), null);
    }

    @Test
    void unparseablePayloadIsSwallowed() {
        service.handleNotification(bytes("this is not json"));

        verifyNoInteractions(deliveryStatusService);
    }

    @Test
    void nullOrEmptyPayloadIsHandled() {
        service.handleNotification(null);
        service.handleNotification(new byte[0]);

        verifyNoInteractions(deliveryStatusService);
    }
}
