package com.surakshasaathi.ai.controller;

import com.surakshasaathi.ai.service.WhatsAppWebhookService;
import com.surakshasaathi.ai.service.whatsapp.WhatsAppSignatureVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Focused unit test proving the webhook controller never processes a POST whose
 * signature is invalid, and never runs processing for a rejected verification.
 */
class WhatsAppWebhookControllerUnitTest {

    @Mock private WhatsAppSignatureVerifier verifier;
    @Mock private WhatsAppWebhookService webhookService;

    private WhatsAppWebhookController controller;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        controller = new WhatsAppWebhookController(verifier, webhookService);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() throws Exception {
        if (mocks != null) mocks.close();
    }

    @Test
    void invalidSignatureIsRejectedWithoutProcessing() {
        when(verifier.isSignatureValid(any(), any())).thenReturn(false);

        ResponseEntity<String> response = controller.receive("sha256=bad", "{}".getBytes(StandardCharsets.UTF_8));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(webhookService);
    }

    @Test
    void validSignatureProcessesTheEvent() {
        when(verifier.isSignatureValid(any(), any())).thenReturn(true);
        byte[] body = "{\"object\":\"whatsapp_business_account\"}".getBytes(StandardCharsets.UTF_8);

        ResponseEntity<String> response = controller.receive("sha256=good", body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("EVENT_RECEIVED");
        verify(webhookService).handleNotification(body);
    }

    @Test
    void invalidVerificationDoesNotExposeChallenge() {
        when(verifier.isSubscriptionValid(any(), any())).thenReturn(false);

        ResponseEntity<String> response = controller.verify("subscribe", "wrong", "12345");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNull();
        verify(webhookService, never()).handleNotification(any());
    }
}
