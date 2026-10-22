package com.surakshasaathi.ai.service.whatsapp;

import com.surakshasaathi.ai.config.WhatsAppProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Validates Meta WhatsApp Cloud API webhook traffic:
 *
 *   - GET verification: checks {@code hub.mode == subscribe} and the configured
 *     verify token before echoing the challenge.
 *   - POST notifications: recomputes HMAC-SHA256 over the EXACT raw request bytes
 *     using the Meta app secret and compares with {@code X-Hub-Signature-256}
 *     ("sha256=<hex>") using a constant-time comparison.
 *
 * Secrets are read from {@link WhatsAppProperties} (env-backed) and never logged.
 * When a required secret is missing, verification fails closed (returns false).
 */
@Component
public class WhatsAppSignatureVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final String SUBSCRIBE_MODE = "subscribe";

    private final WhatsAppProperties properties;

    public WhatsAppSignatureVerifier(WhatsAppProperties properties) {
        this.properties = properties;
    }

    /**
     * @return true only if {@code signatureHeader} is a valid HMAC-SHA256 of the
     *         raw body under the configured app secret.
     */
    public boolean isSignatureValid(byte[] rawBody, String signatureHeader) {
        String secret = properties.getAppSecret();
        if (rawBody == null || !StringUtils.hasText(secret) || !StringUtils.hasText(signatureHeader)) {
            return false;
        }
        if (!signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            return false;
        }

        byte[] provided = decodeHex(signatureHeader.substring(SIGNATURE_PREFIX.length()));
        if (provided == null) {
            return false;
        }

        byte[] expected = hmacSha256(rawBody, secret);
        return MessageDigest.isEqual(expected, provided);
    }

    /**
     * @return true only if the mode is {@code subscribe} and the provided verify
     *         token matches the configured token (constant-time compare).
     */
    public boolean isSubscriptionValid(String mode, String verifyToken) {
        String expected = properties.getVerifyToken();
        if (!StringUtils.hasText(expected) || verifyToken == null || !SUBSCRIBE_MODE.equals(mode)) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                verifyToken.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] hmacSha256(byte[] data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(data);
        } catch (GeneralSecurityException e) {
            // Should never happen with a fixed, available algorithm.
            throw new IllegalStateException("Unable to compute HMAC-SHA256", e);
        }
    }

    /** Decodes a hex string, returning null when it is not a valid even-length hex value. */
    private static byte[] decodeHex(String hex) {
        try {
            return HexFormat.of().parseHex(hex);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
