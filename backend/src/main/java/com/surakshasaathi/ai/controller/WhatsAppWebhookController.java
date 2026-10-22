package com.surakshasaathi.ai.controller;

import com.surakshasaathi.ai.service.WhatsAppWebhookService;
import com.surakshasaathi.ai.service.whatsapp.WhatsAppSignatureVerifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Meta WhatsApp Cloud API webhook endpoint.
 *
 *   GET  /api/v1/webhooks/whatsapp  -> verification challenge (public)
 *   POST /api/v1/webhooks/whatsapp  -> event notifications (public, signature-gated)
 *
 * GET validates hub.mode and the verify token before echoing the challenge.
 * POST validates X-Hub-Signature-256 over the exact raw request bytes; an invalid
 * signature is rejected WITHOUT processing the event.
 *
 * This path is the ONLY new public surface; every existing application route stays
 * JWT-protected (see SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/webhooks/whatsapp")
public class WhatsAppWebhookController {

    private static final String SIGNATURE_HEADER = "X-Hub-Signature-256";

    private final WhatsAppSignatureVerifier signatureVerifier;
    private final WhatsAppWebhookService webhookService;

    public WhatsAppWebhookController(WhatsAppSignatureVerifier signatureVerifier,
                                     WhatsAppWebhookService webhookService) {
        this.signatureVerifier = signatureVerifier;
        this.webhookService = webhookService;
    }

    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam(value = "hub.mode", required = false) String mode,
            @RequestParam(value = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(value = "hub.challenge", required = false) String challenge) {

        if (!signatureVerifier.isSubscriptionValid(mode, verifyToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (challenge == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(challenge);
    }

    @PostMapping
    public ResponseEntity<String> receive(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody(required = false) byte[] rawBody) {

        if (!signatureVerifier.isSignatureValid(rawBody, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        webhookService.handleNotification(rawBody);
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("EVENT_RECEIVED");
    }
}
