package com.surakshasaathi.ai.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-context web-layer test for the WhatsApp webhook endpoint, including the
 * security contract: the webhook paths are public, and every existing protected
 * application route still requires a JWT.
 */
@SpringBootTest(properties = {
        "app.whatsapp.app-secret=test-app-secret",
        "app.whatsapp.verify-token=test-verify-token"
})
@AutoConfigureMockMvc
class WhatsAppWebhookControllerTest {

    private static final String APP_SECRET = "test-app-secret";
    private static final String VERIFY_TOKEN = "test-verify-token";
    private static final String WEBHOOK_PATH = "/api/v1/webhooks/whatsapp";
    private static final byte[] EVENT_BODY =
            "{\"object\":\"whatsapp_business_account\",\"entry\":[]}".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private MockMvc mvc;

    private static String sign(byte[] body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
    }

    // ---------------------------------------------------------------
    // GET verification
    // ---------------------------------------------------------------

    @Test
    void getVerificationEchoesChallengeWhenTokenMatches() throws Exception {
        mvc.perform(get(WEBHOOK_PATH)
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", VERIFY_TOKEN)
                        .param("hub.challenge", "1234567890"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("1234567890"));
    }

    @Test
    void getVerificationRejectsWrongToken() throws Exception {
        mvc.perform(get(WEBHOOK_PATH)
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "wrong-token")
                        .param("hub.challenge", "1234567890"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getVerificationRejectsWrongMode() throws Exception {
        mvc.perform(get(WEBHOOK_PATH)
                        .param("hub.mode", "unsubscribe")
                        .param("hub.verify_token", VERIFY_TOKEN)
                        .param("hub.challenge", "1234567890"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getVerificationRejectsMissingChallenge() throws Exception {
        mvc.perform(get(WEBHOOK_PATH)
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", VERIFY_TOKEN))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------
    // POST notifications
    // ---------------------------------------------------------------

    @Test
    void postAcceptsValidSignature() throws Exception {
        mvc.perform(post(WEBHOOK_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", sign(EVENT_BODY, APP_SECRET))
                        .content(EVENT_BODY))
                .andExpect(status().isOk())
                .andExpect(content().string("EVENT_RECEIVED"));
    }

    @Test
    void postRejectsTamperedBody() throws Exception {
        byte[] tampered = "{\"object\":\"whatsapp_business_account\",\"entry\":[{\"id\":\"x\"}]}"
                .getBytes(StandardCharsets.UTF_8);
        String signatureOfOriginal = sign(EVENT_BODY, APP_SECRET);

        mvc.perform(post(WEBHOOK_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", signatureOfOriginal)
                        .content(tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postRejectsMissingSignatureHeader() throws Exception {
        mvc.perform(post(WEBHOOK_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EVENT_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postRejectsSignatureFromWrongSecret() throws Exception {
        mvc.perform(post(WEBHOOK_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", sign(EVENT_BODY, "some-other-secret"))
                        .content(EVENT_BODY))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------
    // Security regression
    // ---------------------------------------------------------------

    @Test
    void webhookIsPublicButProtectedRoutesStillRequireJwt() throws Exception {
        // Webhook is reachable without a JWT (proves the public matcher).
        mvc.perform(get(WEBHOOK_PATH)
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", VERIFY_TOKEN)
                        .param("hub.challenge", "1"))
                .andExpect(status().isOk());

        // Existing protected application routes are unchanged.
        mvc.perform(get("/api/v1/trusted-circle/contacts"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/trusted-circle/alert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactName\":\"Ravi\",\"contactPhone\":\"+10000000000\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/analyze/text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Your KYC has expired\",\"language\":\"en\"}"))
                .andExpect(status().isUnauthorized());
    }
}
