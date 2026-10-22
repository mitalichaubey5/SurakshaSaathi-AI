package com.surakshasaathi.ai.service.rule;

import com.surakshasaathi.ai.dto.analysis.RiskLevel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleEngineTest {

    private final RuleEngine engine = new RuleEngine();

    @Test
    void kycScamDetected() {
        RuleResult result = engine.detect(
                "Your KYC has been suspended. Please update your KYC immediately or your account will be blocked.");
        assertThat(result.signals()).extracting(ScamSignal::code)
                .contains("kyc_update", "account_blocked", "urgency");
        assertThat(result.preliminaryRisk()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void otpShareRequestDetected() {
        RuleResult result = engine.detect(
                "Dear customer, your transaction is on hold. Please share the OTP sent to your phone number.");
        assertThat(result.signals()).extracting(ScamSignal::code).contains("otp_share");
        assertThat(result.preliminaryRisk()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void upiPinRequestDetected() {
        RuleResult result = engine.detect(
                "To receive your cashback, please share your UPI PIN with our support team.");
        assertThat(result.signals()).extracting(ScamSignal::code).contains("upi_pin");
        assertThat(result.preliminaryRisk()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void urgentAccountBlockedDetected() {
        RuleResult result = engine.detect(
                "URGENT: Your bank account has been blocked due to suspicious activity. "
                        + "Click here to reactivate within 24 hours or it will be closed.");
        assertThat(result.signals()).extracting(ScamSignal::code).contains("account_blocked", "urgency");
        assertThat(result.preliminaryRisk()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void shortenedUrlNeedsVerification() {
        RuleResult result = engine.detect("Watch this interesting video https://bit.ly/watch999");
        assertThat(result.signals()).extracting(ScamSignal::code).contains("shortened_url");
        assertThat(result.preliminaryRisk()).isEqualTo(RiskLevel.MEDIUM);
    }

    @Test
    void harmlessMessageIsLowRisk() {
        RuleResult result = engine.detect("Hello Uncle, this is Ravi. I have reached home. Please call me when you are free.");
        assertThat(result.signals()).isEmpty();
        assertThat(result.preliminaryRisk()).isEqualTo(RiskLevel.LOW);
    }

    @Test
    void scamTldIsHighRisk() {
        RuleResult result = engine.detect("Open this link to collect your cash: http://reward5432.xyz/claim");
        assertThat(result.signals()).extracting(ScamSignal::code).contains("suspicious_tld");
        assertThat(result.preliminaryRisk()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void lookalikeDomainIsHighRisk() {
        RuleResult result = engine.detect("Verify your bank account now at http://secure-bank-verify.com/login");
        assertThat(result.signals()).extracting(ScamSignal::code).contains("lookalike_domain");
        assertThat(result.preliminaryRisk()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void rewardWithShortenedUrlIsHighRisk() {
        RuleResult result = engine.detect(
                "Congratulations! You have won a free trip. Claim now: https://shorturl.at/abc123");
        assertThat(result.signals()).extracting(ScamSignal::code).contains("reward_prize", "shortened_url");
        assertThat(result.preliminaryRisk()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void blankInputThrows() {
        assertThatThrownBy(() -> engine.detect("  ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> engine.detect(null)).isInstanceOf(IllegalArgumentException.class);
    }
}