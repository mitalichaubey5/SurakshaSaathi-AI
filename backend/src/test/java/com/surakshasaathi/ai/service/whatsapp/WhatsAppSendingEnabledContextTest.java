package com.surakshasaathi.ai.service.whatsapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the conditional wiring swaps in the real client when enabled and that
 * the two conditional beans do not conflict (no duplicate-bean failure).
 */
@SpringBootTest(properties = "app.whatsapp.enabled=true")
class WhatsAppSendingEnabledContextTest {

    @Autowired
    private WhatsAppCloudClient whatsAppCloudClient;

    @Test
    void realClientIsActiveWhenEnabled() {
        assertThat(whatsAppCloudClient).isInstanceOf(MetaWhatsAppCloudClient.class);
    }
}
