package com.surakshasaathi.ai.service.whatsapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surakshasaathi.ai.config.WhatsAppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit tests for the real WhatsApp client, covering acceptance, provider errors,
 * timeouts/ambiguous outcomes, and the missing-credentials guard. No network call
 * is ever made: {@link MockRestServiceServer} intercepts the request.
 */
class MetaWhatsAppCloudClientTest {

    private static final String ENDPOINT = "https://graph.facebook.com/v21.0/1234567890/messages";
    private static final String RECIPIENT = "+15550001111";

    private WhatsAppProperties properties;
    private ObjectMapper objectMapper;
    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        properties = new WhatsAppProperties();
        properties.setAccessToken("test-token");
        properties.setPhoneNumberId("1234567890");
        properties.setGraphBaseUrl("https://graph.facebook.com");
        properties.setApiVersion("v21.0");
        properties.setTemplateName("hello_world");
        properties.setTemplateLanguage("en_US");

        builder = RestClient.builder().baseUrl("https://graph.facebook.com");
        server = MockRestServiceServer.bindTo(builder).build();
    }

    private MetaWhatsAppCloudClient client() {
        return new MetaWhatsAppCloudClient(properties, objectMapper, builder.build());
    }

    private TemplateMessage message() {
        return new TemplateMessage(RECIPIENT, "hello_world", "en_US");
    }

    @Test
    void acceptedResponseIsReportedAsAcceptedNotDelivered() {
        server.expect(requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(jsonPath("$.messaging_product").value("whatsapp"))
                .andExpect(jsonPath("$.to").value(RECIPIENT))
                .andExpect(jsonPath("$.type").value("template"))
                .andExpect(jsonPath("$.template.name").value("hello_world"))
                .andExpect(jsonPath("$.template.language.code").value("en_US"))
                .andRespond(withSuccess("{\"messages\":[{\"id\":\"wamid.TEST123\"}]}", MediaType.APPLICATION_JSON));

        SendResult result = client().sendTemplate(message());

        assertThat(result.outcome()).isEqualTo(SendResult.Outcome.ACCEPTED);
        assertThat(result.providerMessageId()).isEqualTo("wamid.TEST123");
        assertThat(result.errorCode()).isNull();
        server.verify();
    }

    @Test
    void providerErrorYieldsFailedWithCodeAndNoRawBody() {
        server.expect(requestTo(ENDPOINT))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"code\":131030,\"message\":\"recipient +15550001111 not in allowed list\"}}"));

        SendResult result = client().sendTemplate(message());

        assertThat(result.outcome()).isEqualTo(SendResult.Outcome.FAILED);
        assertThat(result.errorCode()).isEqualTo("131030");
        assertThat(result.providerMessageId()).isNull();
        assertThat(result.errorSummary())
                .doesNotContain("131030")
                .doesNotContain(RECIPIENT)
                .doesNotContain("allowed list");
    }

    @Test
    void serverErrorYieldsFailed() {
        server.expect(requestTo(ENDPOINT))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"boom\"}}"));

        SendResult result = client().sendTemplate(message());

        assertThat(result.outcome()).isEqualTo(SendResult.Outcome.FAILED);
        assertThat(result.errorCode()).isNull();
    }

    @Test
    void timeoutIsReportedAsAmbiguousNotFailed() {
        // A request factory that fails with an I/O timeout on every call.
        ClientHttpRequestFactory failing = (uri, httpMethod) -> {
            throw new SocketTimeoutException("simulated timeout");
        };
        RestClient failingClient = RestClient.builder()
                .baseUrl("https://graph.facebook.com")
                .requestFactory(failing)
                .build();

        MetaWhatsAppCloudClient client = new MetaWhatsAppCloudClient(properties, objectMapper, failingClient);

        SendResult result = client.sendTemplate(message());

        assertThat(result.outcome()).isEqualTo(SendResult.Outcome.TIMEOUT);
        assertThat(result.providerMessageId()).isNull();
    }

    @Test
    void missingCredentialsAreDisabledWithoutNetworkCall() {
        properties.setAccessToken("");

        SendResult result = client().sendTemplate(message());

        assertThat(result.outcome()).isEqualTo(SendResult.Outcome.DISABLED);
        server.verify();
    }

    @Test
    void missingPhoneNumberIdIsDisabledWithoutNetworkCall() {
        properties.setPhoneNumberId("");

        SendResult result = client().sendTemplate(message());

        assertThat(result.outcome()).isEqualTo(SendResult.Outcome.DISABLED);
        server.verify();
    }

    @Test
    void successWithoutMessageIdIsTreatedAsFailed() {
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess("{\"messages\":[]}", MediaType.APPLICATION_JSON));

        SendResult result = client().sendTemplate(message());

        assertThat(result.outcome()).isEqualTo(SendResult.Outcome.FAILED);
        assertThat(result.providerMessageId()).isNull();
    }
}
