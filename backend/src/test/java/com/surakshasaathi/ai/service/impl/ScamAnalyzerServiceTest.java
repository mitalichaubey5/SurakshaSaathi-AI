package com.surakshasaathi.ai.service.impl;

import com.surakshasaathi.ai.dto.analysis.AnalysisResponse;
import com.surakshasaathi.ai.dto.analysis.LlmVerdict;
import com.surakshasaathi.ai.dto.analysis.RiskLevel;
import com.surakshasaathi.ai.service.LlmPromptService;
import com.surakshasaathi.ai.service.rule.RuleEngine;
import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end analyzer tests for the 7 required scenarios plus the
 * safe rule+LLM merge behaviour.
 */
class ScamAnalyzerServiceTest {

    private static final String KYC_MSG =
            "Your KYC has been suspended. Please update your KYC immediately or your account will be blocked.";
    private static final String OTP_MSG =
            "Dear customer, your transaction is on hold. Please share the OTP sent to your phone number.";
    private static final String UPI_PIN_MSG =
            "To receive your cashback, please share your UPI PIN with our support team.";
    private static final String ACCOUNT_BLOCKED_MSG =
            "URGENT: Your bank account has been blocked due to suspicious activity. "
                    + "Click here to reactivate within 24 hours or it will be closed.";
    private static final String SHORT_URL_MSG = "Watch this interesting video https://bit.ly/watch999";
    private static final String HARMLESS_MSG =
            "Hello Uncle, this is Ravi. I have reached home. Please call me when you are free.";

    private static final LlmPromptService NO_LLM = (text, signals, lang) -> Optional.empty();

    private static LlmPromptService returning(LlmVerdict verdict) {
        return (text, signals, lang) -> Optional.ofNullable(verdict);
    }

    private static com.surakshasaathi.ai.service.ScamAnalyzerService service(LlmPromptService llm) {
        return new ScamAnalyzerServiceImpl(new RuleEngine(), llm);
    }

    // ---------------------------------------------------------------
    // 1. KYC scam
    // ---------------------------------------------------------------
    @Test
    void kycScamIsHighRisk() {
        AnalysisResponse r = service(NO_LLM).analyzeText(KYC_MSG, "en");
        assertThat(r.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(r.matchedSignals()).anyMatch(s -> s.contains("KYC"));
        assertThat(r.analysisSource()).isEqualTo("RULE_ENGINE");
    }

    // ---------------------------------------------------------------
    // 2. OTP request
    // ---------------------------------------------------------------
    @Test
    void otpRequestIsHighRisk() {
        AnalysisResponse r = service(NO_LLM).analyzeText(OTP_MSG, "en");
        assertThat(r.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(r.prohibitedActions()).anyMatch(a -> a.toLowerCase().contains("otp"));
    }

    // ---------------------------------------------------------------
    // 3. UPI PIN request
    // ---------------------------------------------------------------
    @Test
    void upiPinRequestIsHighRisk() {
        AnalysisResponse r = service(NO_LLM).analyzeText(UPI_PIN_MSG, "en");
        assertThat(r.riskLevel()).isEqualTo(RiskLevel.HIGH);
    }

    // ---------------------------------------------------------------
    // 4. Urgent account-blocked message
    // ---------------------------------------------------------------
    @Test
    void urgentAccountBlockedIsHighRisk() {
        AnalysisResponse r = service(NO_LLM).analyzeText(ACCOUNT_BLOCKED_MSG, "en");
        assertThat(r.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(r.safeActions()).isNotEmpty();
    }

    // ---------------------------------------------------------------
    // 5. Suspicious shortened URL
    // ---------------------------------------------------------------
    @Test
    void shortenedUrlNeedsVerification() {
        AnalysisResponse r = service(NO_LLM).analyzeText(SHORT_URL_MSG, "en");
        assertThat(r.riskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(r.matchedSignals()).anyMatch(s -> s.toLowerCase().contains("shorten"));
    }

    // ---------------------------------------------------------------
    // 6. Normal harmless message
    // ---------------------------------------------------------------
    @Test
    void harmlessMessageIsLowRisk() {
        AnalysisResponse r = service(NO_LLM).analyzeText(HARMLESS_MSG, "en");
        assertThat(r.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(r.matchedSignals()).isEmpty();
        assertThat(r.prohibitedActions()).anyMatch(a -> a.toLowerCase().contains("do not"));
    }

    // ---------------------------------------------------------------
    // 7. Invalid / empty input
    // ---------------------------------------------------------------
    @Test
    void emptyInputThrows() {
        var svc = service(NO_LLM);
        assertThatThrownBy(() -> svc.analyzeText("   ", "en")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> svc.analyzeText(null, "en")).isInstanceOf(IllegalArgumentException.class);
    }

    // ---------------------------------------------------------------
    // Safe merge: LLM can never lower a strong rule-engine signal
    // ---------------------------------------------------------------
    @Test
    void llmCannotLowerStrongRuleSignal() {
        LlmVerdict tooLenient = new LlmVerdict(
                RiskLevel.LOW,
                "This looks fine, you can ignore it.",
                List.of("Ignore the message"),
                List.of(),
                "Nothing to do");
        AnalysisResponse r = service(returning(tooLenient)).analyzeText(KYC_MSG, "en");

        assertThat(r.riskLevel()).isEqualTo(RiskLevel.HIGH); // rule engine wins
        assertThat(r.simpleExplanation()).isEqualTo("This looks fine, you can ignore it."); // LLM text still used
        assertThat(r.analysisSource()).isEqualTo("RULE_ENGINE + LLM");
    }

    @Test
    void llmMayRaiseRiskWhenRuleEngineIsUnsure() {
        LlmVerdict careful = new LlmVerdict(
                RiskLevel.HIGH,
                "This message asks for your personal details. Treat it as dangerous.",
                List.of("Do not reply"),
                List.of("Do not share details"),
                "Verify on the official website");
        AnalysisResponse r = service(returning(careful)).analyzeText(SHORT_URL_MSG, "en");

        assertThat(r.riskLevel()).isEqualTo(RiskLevel.HIGH); // LLM raised MEDIUM -> HIGH
        assertThat(r.simpleExplanation()).contains("dangerous");
    }

    // ---------------------------------------------------------------
    // LLM unavailable -> rule-engine result stays usable
    // ---------------------------------------------------------------
    @Test
    void llmUnavailableFallsBackToRuleEngine() {
        AnalysisResponse r = service(NO_LLM).analyzeText(ACCOUNT_BLOCKED_MSG, "hi");

        assertThat(r.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(r.analysisSource()).isEqualTo("RULE_ENGINE");
        assertThat(r.simpleExplanation()).isNotBlank();
        assertThat(r.safeActions()).isNotEmpty();
        assertThat(r.prohibitedActions()).isNotEmpty();
        assertThat(StringUtils.hasText(r.verificationSteps())).isTrue();
    }

    // ---------------------------------------------------------------
    // Image path routes through the same engine
    // ---------------------------------------------------------------
    @Test
    void screenshotAnalysisUsesImagePipeline() {
        AnalysisResponse r = service(NO_LLM).analyzeOcrText(UPI_PIN_MSG, "en", "raw ocr text from screenshot");
        assertThat(r.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(r.inputType()).isEqualTo("IMAGE");
        assertThat(r.ocrText()).isEqualTo("raw ocr text from screenshot");
    }

    // ---------------------------------------------------------------
    // Hindi fallback guidance
    // ---------------------------------------------------------------
    @Test
    void hindiFallbackGuidanceIsHinglish() {
        AnalysisResponse r = service(NO_LLM).analyzeText(ACCOUNT_BLOCKED_MSG, "hi");
        assertThat(r.simpleExplanation()).contains("khatarnak");
        assertThat(r.prohibitedActions()).anyMatch(a -> a.contains("na dabayein"));
    }
}