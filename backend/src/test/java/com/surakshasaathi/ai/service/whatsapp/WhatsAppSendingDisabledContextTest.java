package com.surakshasaathi.ai.service.whatsapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Default context wiring: with {@code app.whatsapp.enabled} unset, only the
 * no-op client exists, so no outbound HTTP client is ever created.
 */
@SpringBootTest
class WhatsAppSendingDisabledContextTest {

    @Autowired
    private WhatsAppCloudClient whatsAppCloudClient;

    @Test
    void disabledClientIsActiveByDefault() {
        assertThat(whatsAppCloudClient).isInstanceOf(DisabledWhatsAppCloudClient.class);
    }
}
