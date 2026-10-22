package com.surakshasaathi.ai.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the strict E.164-style configured-recipient validation.
 *
 * <p>Accepted: a leading '+' then 8-15 digits, first digit non-zero.
 * Everything else (missing '+', spaces, letters, punctuation, wrong length,
 * leading zero, null) is rejected and never normalized.</p>
 */
class WhatsAppRecipientFormatTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "+15550001111",         // 11 digits
            "+441234567890",        // 12 digits
            "+919876543210",        // 12 digits
            "+12345678",            // minimum: 8 digits
            "+123456789012345"      // maximum: 15 digits
    })
    void acceptsValidE164StyleNumbers(String value) {
        assertThat(TrustedCircleServiceImpl.isValidTestRecipient(value)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",                     // empty
            "   ",                  // whitespace only
            "15550001111",          // missing leading '+'
            "+1 5550001111",        // embedded space
            "+1555 ABC 1111",       // multiple embedded spaces
            "+1555ABC1111",         // letters
            "+1555-000-1111",       // punctuation other than the leading '+'
            "(+1555)0001111",       // wrapping punctuation
            "+1234567",             // too few digits (7)
            "+1234567890123456",    // too many digits (16)
            "+05550001111"          // first digit after '+' is zero
    })
    void rejectsInvalidNumbers(String value) {
        assertThat(TrustedCircleServiceImpl.isValidTestRecipient(value)).isFalse();
    }

    @ParameterizedTest
    @NullSource
    void rejectsNull(String value) {
        assertThat(TrustedCircleServiceImpl.isValidTestRecipient(value)).isFalse();
    }

    @Test
    void boundaryLengthsAreEnforcedPrecisely() {
        assertThat(TrustedCircleServiceImpl.isValidTestRecipient("+1234567")).isFalse();
        assertThat(TrustedCircleServiceImpl.isValidTestRecipient("+12345678")).isTrue();
        assertThat(TrustedCircleServiceImpl.isValidTestRecipient("+123456789012345")).isTrue();
        assertThat(TrustedCircleServiceImpl.isValidTestRecipient("+1234567890123456")).isFalse();
    }
}
