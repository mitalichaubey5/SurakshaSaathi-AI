package com.surakshasaathi.ai.service.whatsapp;

import com.surakshasaathi.ai.config.WhatsAppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsAppSignatureVerifierTest {

    private static final String APP_SECRET = "test-app-secret";
    private static final String VERIFY_TOKEN = "test-verify-token";

    private WhatsAppSignatureVerifier verifier;

    @BeforeEach
    void setUp() {
        WhatsAppProperties props = new WhatsAppProperties();
        props.setAppSecret(APP_SECRET);
        props.setVerifyToken(VERIFY_TOKEN);
        verifier = new WhatsAppSignatureVerifier(props);
    }

    private static String sign(byte[] body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void acceptsValidSignature() throws Exception {
        byte[] body = "{\"object\":\"whatsapp_business_account\"}".getBytes(StandardCharsets.UTF_8);
        assertThat(verifier.isSignatureValid(body, sign(body, APP_SECRET))).isTrue();
    }

    @Test
    void rejectsTamperedBody() throws Exception {
        byte[] original = "{\"object\":\"whatsapp_business_account\"}".getBytes(StandardCharsets.UTF_8);
        byte[] tampered = "{\"object\":\"whatsapp_business_account\",\"entry\":[]}".getBytes(StandardCharsets.UTF_8);
        String signatureOfOriginal = sign(original, APP_SECRET);

        assertThat(verifier.isSignatureValid(tampered, signatureOfOriginal)).isFalse();
    }

    @Test
    void rejectsSignatureFromWrongSecret() throws Exception {
        byte[] body = bytes("{\"hello\":\"world\"}");
        String wrongSignature = sign(body, "some-other-secret");

        assertThat(verifier.isSignatureValid(body, wrongSignature)).isFalse();
    }

    @Test
    void rejectsMissingSignatureHeader() {
        byte[] body = bytes("{}");
        assertThat(verifier.isSignatureValid(body, null)).isFalse();
        assertThat(verifier.isSignatureValid(body, "")).isFalse();
    }

    @Test
    void rejectsSignatureWithoutSha256Prefix() throws Exception {
        byte[] body = bytes("{\"a\":1}");
        String hexOnly = sign(body, APP_SECRET).substring("sha256=".length());

        assertThat(verifier.isSignatureValid(body, hexOnly)).isFalse();
    }

    @Test
    void rejectsMalformedHexSignature() {
        byte[] body = bytes("{\"a\":1}");
        assertThat(verifier.isSignatureValid(body, "sha256=not-valid-hex")).isFalse();
    }

    @Test
    void rejectsEverythingWhenAppSecretIsBlank() {
        WhatsAppProperties props = new WhatsAppProperties();
        props.setAppSecret("");
        WhatsAppSignatureVerifier noSecretVerifier = new WhatsAppSignatureVerifier(props);
        byte[] body = bytes("{}");

        assertThat(noSecretVerifier.isSignatureValid(body, "sha256=00")).isFalse();
    }

    @Test
    void acceptsValidSubscription() {
        assertThat(verifier.isSubscriptionValid("subscribe", "test-verify-token")).isTrue();
    }

    @Test
    void rejectsWrongVerifyToken() {
        assertThat(verifier.isSubscriptionValid("subscribe", "wrong-token")).isFalse();
    }

    @Test
    void rejectsWrongMode() {
        assertThat(verifier.isSubscriptionValid("unsubscribe", "test-verify-token")).isFalse();
    }

    @Test
    void rejectsNullVerifyToken() {
        assertThat(verifier.isSubscriptionValid("subscribe", null)).isFalse();
    }

    @Test
    void rejectsSubscriptionWhenVerifyTokenIsUnset() {
        WhatsAppProperties props = new WhatsAppProperties();
        props.setVerifyToken("");
        WhatsAppSignatureVerifier noTokenVerifier = new WhatsAppSignatureVerifier(props);

        assertThat(noTokenVerifier.isSubscriptionValid("subscribe", "anything")).isFalse();
    }
}
