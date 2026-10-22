package com.surakshasaathi.ai.service.whatsapp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the forward-only delivery-status rules and Meta status mapping.
 */
class DeliveryStatusTest {

    @Test
    void mapsMetaStatusStrings() {
        assertThat(DeliveryStatus.fromMetaStatus("sent")).isEqualTo(DeliveryStatus.SENT);
        assertThat(DeliveryStatus.fromMetaStatus("DELIVERED")).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(DeliveryStatus.fromMetaStatus(" read ")).isEqualTo(DeliveryStatus.READ);
        assertThat(DeliveryStatus.fromMetaStatus("failed")).isEqualTo(DeliveryStatus.FAILED);
        assertThat(DeliveryStatus.fromMetaStatus("queued")).isNull();
        assertThat(DeliveryStatus.fromMetaStatus(null)).isNull();
        assertThat(DeliveryStatus.fromMetaStatus("  ")).isNull();
    }

    @Test
    void parsesStoredNamesLeniently() {
        assertThat(DeliveryStatus.parse("accepted")).isEqualTo(DeliveryStatus.ACCEPTED);
        assertThat(DeliveryStatus.parse("DELIVERED")).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(DeliveryStatus.parse("nonsense")).isNull();
        assertThat(DeliveryStatus.parse(null)).isNull();
    }

    @Test
    void forwardProgressionIsAllowed() {
        assertThat(DeliveryStatus.SENT.shouldApplyAfter(DeliveryStatus.ACCEPTED)).isTrue();
        assertThat(DeliveryStatus.DELIVERED.shouldApplyAfter(DeliveryStatus.SENT)).isTrue();
        assertThat(DeliveryStatus.READ.shouldApplyAfter(DeliveryStatus.DELIVERED)).isTrue();
        assertThat(DeliveryStatus.DELIVERED.shouldApplyAfter(DeliveryStatus.ACCEPTED)).isTrue();
        assertThat(DeliveryStatus.SENT.shouldApplyAfter(null)).isTrue();
    }

    @Test
    void backwardAndDuplicateUpdatesAreRejected() {
        assertThat(DeliveryStatus.DELIVERED.shouldApplyAfter(DeliveryStatus.DELIVERED)).isFalse();
        assertThat(DeliveryStatus.SENT.shouldApplyAfter(DeliveryStatus.DELIVERED)).isFalse();
        assertThat(DeliveryStatus.READ.shouldApplyAfter(DeliveryStatus.READ)).isFalse();
        assertThat(DeliveryStatus.ACCEPTED.shouldApplyAfter(DeliveryStatus.SENT)).isFalse();
    }

    @Test
    void failureCanReplaceNonDeliveredStatesOnly() {
        assertThat(DeliveryStatus.FAILED.shouldApplyAfter(DeliveryStatus.ACCEPTED)).isTrue();
        assertThat(DeliveryStatus.FAILED.shouldApplyAfter(DeliveryStatus.SENT)).isTrue();
        assertThat(DeliveryStatus.FAILED.shouldApplyAfter(DeliveryStatus.DELIVERED)).isFalse();
        assertThat(DeliveryStatus.FAILED.shouldApplyAfter(DeliveryStatus.READ)).isFalse();
        assertThat(DeliveryStatus.DELIVERED.shouldApplyAfter(DeliveryStatus.FAILED)).isFalse();
        assertThat(DeliveryStatus.SENT.shouldApplyAfter(DeliveryStatus.FAILED)).isFalse();
    }

    @Test
    void terminalAndNonWebhookStatusesAreNeverAdvancedByEvents() {
        assertThat(DeliveryStatus.READ.shouldApplyAfter(DeliveryStatus.READ)).isFalse();
        assertThat(DeliveryStatus.MOCK.shouldApplyAfter(DeliveryStatus.ACCEPTED)).isFalse();
        assertThat(DeliveryStatus.UNKNOWN.shouldApplyAfter(DeliveryStatus.ACCEPTED)).isFalse();
        assertThat(DeliveryStatus.ACCEPTED.shouldApplyAfter(DeliveryStatus.REJECTED_RECIPIENT)).isFalse();
        assertThat(DeliveryStatus.SENT.shouldApplyAfter(DeliveryStatus.FAILED)).isFalse();
    }

    @Test
    void failedCannotBeDowngradedFromTerminalStates() {
        assertThat(DeliveryStatus.FAILED.shouldApplyAfter(DeliveryStatus.FAILED)).isFalse();
        assertThat(DeliveryStatus.READ.shouldApplyAfter(DeliveryStatus.FAILED)).isFalse();
    }
}
