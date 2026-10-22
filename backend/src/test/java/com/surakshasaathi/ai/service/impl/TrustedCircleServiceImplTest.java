package com.surakshasaathi.ai.service.impl;

import com.surakshasaathi.ai.config.WhatsAppProperties;
import com.surakshasaathi.ai.dto.trusted.TrustedAlertRequest;
import com.surakshasaathi.ai.model.AlertLog;
import com.surakshasaathi.ai.repository.AlertLogRepository;
import com.surakshasaathi.ai.service.whatsapp.DeliveryStatus;
import com.surakshasaathi.ai.service.whatsapp.SendResult;
import com.surakshasaathi.ai.service.whatsapp.TemplateMessage;
import com.surakshasaathi.ai.service.whatsapp.WhatsAppCloudClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the trusted-circle alert flow: unchanged mock behavior when
 * sending is disabled, the allow-list policy, and honest status reporting for
 * accepted / failed / timed-out real sends.
 */
class TrustedCircleServiceImplTest {

    private static final String CONFIGURED_RECIPIENT = "+15550001111";
    private static final String REQUEST_PHONE = "+19998887777";

    private AlertLogRepository repository;
    private WhatsAppProperties properties;
    private WhatsAppCloudClient client;
    private TrustedCircleServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(AlertLogRepository.class);
        properties = new WhatsAppProperties();
        client = mock(WhatsAppCloudClient.class);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service = new TrustedCircleServiceImpl(repository, properties, client);
    }

    private void enableRealSending() {
        properties.setEnabled(true);
        properties.setSendingEnabled(true);
        properties.setAccessToken("test-token");
        properties.setPhoneNumberId("1234567890");
        properties.setTestRecipient(CONFIGURED_RECIPIENT);
    }

    private TrustedAlertRequest request() {
        return new TrustedAlertRequest("Ravi", REQUEST_PHONE, "Suspicious KYC message", "HIGH", "en");
    }

    // ------------------------------------------------------------------
    // Disabled / fallback behavior (must be unchanged)
    // ------------------------------------------------------------------

    @Test
    void defaultConfigUsesMockAndNeverCallsTheClient() {
        var response = service.sendAlert(request());

        assertThat(response.delivered()).isTrue();
        assertThat(response.mockChannel()).isEqualTo("MOCK_SMS + MOCK_PUSH");
        assertThat(response.status()).isEqualTo(DeliveryStatus.MOCK.name());
        assertThat(response.providerMessageId()).isNull();
        verifyNoInteractions(client);
        verify(repository).save(any(AlertLog.class));
    }

    @Test
    void enabledButSendingDisabledStillUsesMock() {
        properties.setEnabled(true);
        properties.setAccessToken("test-token");
        properties.setPhoneNumberId("1234567890");
        properties.setTestRecipient(CONFIGURED_RECIPIENT);

        var response = service.sendAlert(request());

        assertThat(response.delivered()).isTrue();
        assertThat(response.status()).isEqualTo(DeliveryStatus.MOCK.name());
        verifyNoInteractions(client);
    }

    @Test
    void missingCredentialsFallBackToMock() {
        properties.setEnabled(true);
        properties.setSendingEnabled(true);

        var response = service.sendAlert(request());

        assertThat(response.status()).isEqualTo(DeliveryStatus.MOCK.name());
        verifyNoInteractions(client);
    }

    // ------------------------------------------------------------------
    // Allow-list policy
    // ------------------------------------------------------------------

    @Test
    void realSendWithNoConfiguredRecipientIsRejectedWithoutCallingTheClient() {
        properties.setEnabled(true);
        properties.setSendingEnabled(true);
        properties.setAccessToken("test-token");
        properties.setPhoneNumberId("1234567890");

        var response = service.sendAlert(request());

        assertThat(response.status()).isEqualTo(DeliveryStatus.REJECTED_RECIPIENT.name());
        assertThat(response.delivered()).isFalse();
        assertThat(response.providerMessageId()).isNull();
        verifyNoInteractions(client);
    }

    @Test
    void realSendAlwaysTargetsTheConfiguredRecipientNotTheRequestPhone() {
        enableRealSending();
        when(client.sendTemplate(any())).thenReturn(SendResult.accepted("wamid.OK"));

        service.sendAlert(request());

        var captor = ArgumentCaptor.forClass(TemplateMessage.class);
        verify(client).sendTemplate(captor.capture());
        assertThat(captor.getValue().toE164()).isEqualTo(CONFIGURED_RECIPIENT);
        assertThat(captor.getValue().toE164()).isNotEqualTo(REQUEST_PHONE);
        assertThat(captor.getValue().templateName()).isEqualTo("hello_world");
        assertThat(captor.getValue().languageCode()).isEqualTo("en_US");
    }

    @Test
    void recipientListIsAlsoAccepted() {
        properties.setEnabled(true);
        properties.setSendingEnabled(true);
        properties.setAccessToken("test-token");
        properties.setPhoneNumberId("1234567890");
        properties.setTestRecipients(CONFIGURED_RECIPIENT + ",+15550002222");
        when(client.sendTemplate(any())).thenReturn(SendResult.accepted("wamid.OK"));

        var response = service.sendAlert(request());

        assertThat(response.status()).isEqualTo(DeliveryStatus.ACCEPTED.name());
    }

    // ------------------------------------------------------------------
    // Configured-recipient format validation
    // ------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {
            "",                    // empty
            "   ",                 // whitespace only
            "15550001111",         // missing leading '+'
            "+1 5550001111",       // embedded space
            "+1555ABC1111",        // letters
            "+1234567",            // too few digits (7)
            "+1234567890123456",   // too many digits (16)
            "+05550001111"         // first digit after '+' is zero
    })
    void invalidConfiguredRecipientIsRejectedWithoutCallingTheClient(String invalid) {
        properties.setEnabled(true);
        properties.setSendingEnabled(true);
        properties.setAccessToken("test-token");
        properties.setPhoneNumberId("1234567890");
        properties.setTestRecipient(invalid);

        var response = service.sendAlert(request());

        assertThat(response.status()).isEqualTo(DeliveryStatus.REJECTED_RECIPIENT.name());
        assertThat(response.delivered()).isFalse();
        verifyNoInteractions(client);
    }

    @Test
    void recipientWithSurroundingSpacesIsRejectedNotNormalized() {
        properties.setEnabled(true);
        properties.setSendingEnabled(true);
        properties.setAccessToken("test-token");
        properties.setPhoneNumberId("1234567890");
        properties.setTestRecipient(" " + CONFIGURED_RECIPIENT + " ");

        var response = service.sendAlert(request());

        assertThat(response.status()).isEqualTo(DeliveryStatus.REJECTED_RECIPIENT.name());
        verifyNoInteractions(client);
    }

    @Test
    void malformedRecipientIsNeverReplacedByTheRequestPhone() {
        properties.setEnabled(true);
        properties.setSendingEnabled(true);
        properties.setAccessToken("test-token");
        properties.setPhoneNumberId("1234567890");
        properties.setTestRecipient("+1 5550001111");

        service.sendAlert(request());

        verifyNoInteractions(client);
    }

    // ------------------------------------------------------------------
    // Real-send outcomes
    // ------------------------------------------------------------------

    @Test
    void acceptedSendReportsAcceptedNotDeliveredAndStoresWamid() {
        enableRealSending();
        when(client.sendTemplate(any())).thenReturn(SendResult.accepted("wamid.OK"));

        var response = service.sendAlert(request());

        assertThat(response.delivered()).isFalse();
        assertThat(response.status()).isEqualTo(DeliveryStatus.ACCEPTED.name());
        assertThat(response.providerMessageId()).isEqualTo("wamid.OK");
        assertThat(response.confirmationMessage()).contains("accepted");

        ArgumentCaptor<AlertLog> captor = ArgumentCaptor.forClass(AlertLog.class);
        verify(repository, atLeastOnce()).save(captor.capture());
        AlertLog stored = captor.getValue();
        assertThat(stored.getChannel()).isEqualTo("WHATSAPP");
        assertThat(stored.getProviderMessageId()).isEqualTo("wamid.OK");
        assertThat(stored.getDeliveryStatus()).isEqualTo(DeliveryStatus.ACCEPTED.name());
    }

    @Test
    void failedSendReportsFailedAndDoesNotExposeWamid() {
        enableRealSending();
        when(client.sendTemplate(any())).thenReturn(SendResult.failed("131030", "provider rejected the message"));

        var response = service.sendAlert(request());

        assertThat(response.delivered()).isFalse();
        assertThat(response.status()).isEqualTo(DeliveryStatus.FAILED.name());
        assertThat(response.providerMessageId()).isNull();
    }

    @Test
    void timeoutReportsUnknown() {
        enableRealSending();
        when(client.sendTemplate(any())).thenReturn(SendResult.timeout());

        var response = service.sendAlert(request());

        assertThat(response.delivered()).isFalse();
        assertThat(response.status()).isEqualTo(DeliveryStatus.UNKNOWN.name());
        assertThat(response.providerMessageId()).isNull();
    }
}
