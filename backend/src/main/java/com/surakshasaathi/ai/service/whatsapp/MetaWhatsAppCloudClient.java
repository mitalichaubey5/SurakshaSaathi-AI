package com.surakshasaathi.ai.service.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.surakshasaathi.ai.config.WhatsAppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.Map;

/**
 * Real WhatsApp Cloud API client, active only when {@code app.whatsapp.enabled=true}.
 *
 * <p>Security notes:</p>
 * <ul>
 *   <li>Credentials come exclusively from {@link WhatsAppProperties} (env-backed).</li>
 *   <li>Never logs tokens, message contents, recipient numbers, or raw provider bodies.</li>
 *   <li>A 2xx response only means the provider <em>accepted</em> the message
 *       ({@link SendResult.Outcome#ACCEPTED}); delivery is tracked later via webhooks.</li>
 *   <li>Timeouts/ambiguous network failures map to {@link SendResult.Outcome#TIMEOUT}
 *       and are never retried automatically.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(prefix = "app.whatsapp", name = "enabled", havingValue = "true")
public class MetaWhatsAppCloudClient implements WhatsAppCloudClient {

    private static final Logger log = LoggerFactory.getLogger(MetaWhatsAppCloudClient.class);

    private final WhatsAppProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Autowired
    public MetaWhatsAppCloudClient(WhatsAppProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, buildRestClient(properties));
    }

    /** Visible for testing: allows injecting a pre-configured {@link RestClient}. */
    MetaWhatsAppCloudClient(WhatsAppProperties properties, ObjectMapper objectMapper, RestClient restClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClient;
    }

    private static RestClient buildRestClient(WhatsAppProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));
        return RestClient.builder()
                .baseUrl(properties.getGraphBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public SendResult sendTemplate(TemplateMessage message) {
        if (!hasText(properties.getAccessToken()) || !hasText(properties.getPhoneNumberId())) {
            // Defensive: the caller already gates on configuration, but never attempt a
            // request with missing credentials.
            log.warn("WhatsApp send skipped: missing access token or phone number id");
            return SendResult.disabled();
        }

        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", message.toE164(),
                "type", "template",
                "template", Map.of(
                        "name", message.templateName(),
                        "language", Map.of("code", message.languageCode())
                )
        );

        try {
            JsonNode response = restClient.post()
                    .uri("/{version}/{phoneNumberId}/messages",
                            properties.getApiVersion(), properties.getPhoneNumberId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            String providerMessageId = response == null ? null : response.path("messages").path(0).path("id").asText(null);
            if (!hasText(providerMessageId)) {
                log.warn("WhatsApp send returned no message id; treating as failed");
                return SendResult.failed(null, "provider did not return a message id");
            }
            return SendResult.accepted(providerMessageId);
        } catch (RestClientResponseException ex) {
            // Provider rejected the request. Extract only the numeric error code; never
            // log or store the raw provider body.
            String errorCode = extractErrorCode(ex.getResponseBodyAsString());
            log.warn("WhatsApp send rejected by provider: httpStatus={}, hasErrorCode={}",
                    ex.getStatusCode().value(), hasText(errorCode));
            return SendResult.failed(errorCode, "provider rejected the message");
        } catch (ResourceAccessException ex) {
            // Connect/read timeout or other I/O failure: the outcome is ambiguous, so we
            // must not claim failure (and must not retry).
            log.warn("WhatsApp send timed out or hit a network error: {}", ex.getClass().getSimpleName());
            return SendResult.timeout();
        } catch (Exception ex) {
            log.warn("WhatsApp send failed unexpectedly: {}", ex.getClass().getSimpleName());
            return SendResult.failed(null, "unexpected send failure");
        }
    }

    private String extractErrorCode(String responseBody) {
        if (!hasText(responseBody)) {
            return null;
        }
        try {
            JsonNode errorNode = objectMapper.readTree(responseBody).path("error").path("code");
            if (errorNode.isMissingNode() || errorNode.isNull()) {
                return null;
            }
            String code = errorNode.asText(null);
            // Guard against unexpectedly long values landing in a length-constrained column.
            return code != null && code.length() <= 20 ? code : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
