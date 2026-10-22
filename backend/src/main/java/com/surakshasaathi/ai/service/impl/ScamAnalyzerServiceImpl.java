package com.surakshasaathi.ai.service.impl;

import com.surakshasaathi.ai.dto.analysis.AnalysisResponse;
import com.surakshasaathi.ai.dto.analysis.LlmVerdict;
import com.surakshasaathi.ai.dto.analysis.RiskLevel;
import com.surakshasaathi.ai.service.LlmPromptService;
import com.surakshasaathi.ai.service.ScamAnalyzerService;
import com.surakshasaathi.ai.service.rule.RuleEngine;
import com.surakshasaathi.ai.service.rule.RuleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Core analysis pipeline: Detect -> Explain -> Verify -> Guide.
 *
 *   1. Rule engine detects suspicious signals and computes a preliminary risk
 *      (can never be ignored).
 *   2. LLM produces an elderly-first explanation (skipped gracefully when
 *      unavailable or unparseable — the rule-engine result is always usable).
 *   3. Risk levels are merged SAFELY: the final risk is the MAXIMUM of the
 *      rule-engine risk and the LLM risk. The LLM can never lower a strong
 *      rule-engine signal (e.g. KYC+OTP -> HIGH stays HIGH even if the model
 *      says LOW).
 *   4. Text guidance comes from the LLM when valid, otherwise from safe,
 *      bilingual fallback templates.
 */
@Service
public class ScamAnalyzerServiceImpl implements ScamAnalyzerService {

    private static final Logger log = LoggerFactory.getLogger(ScamAnalyzerServiceImpl.class);

    private final RuleEngine ruleEngine;
    private final LlmPromptService llmPromptService;

    public ScamAnalyzerServiceImpl(RuleEngine ruleEngine, LlmPromptService llmPromptService) {
        this.ruleEngine = ruleEngine;
        this.llmPromptService = llmPromptService;
    }

    @Override
    public AnalysisResponse analyzeText(String text, String language) {
        requireText(text);
        return analyze(text, language, "TEXT", null);
    }

    @Override
    public AnalysisResponse analyzeOcrText(String ocrText, String language, String originalOcrText) {
        requireText(ocrText);
        return analyze(ocrText, language, "IMAGE", originalOcrText);
    }

    private AnalysisResponse analyze(String text, String language, String inputType, String ocrRawText) {
        boolean hindi = "hi".equalsIgnoreCase(language);
        RuleResult rule = ruleEngine.detect(text);

        Optional<LlmVerdict> llmVerdict;
        try {
            llmVerdict = llmPromptService.getVerdict(text, rule.signalCodes(), language);
        } catch (Exception ex) {
            log.warn("Rule engine worked but LLM service threw; continuing with rule result only. {}", ex.getMessage());
            llmVerdict = Optional.empty();
        }

        RiskLevel llmRisk = llmVerdict.map(LlmVerdict::riskLevel).orElse(null);
        RiskLevel finalRisk = combineRisk(rule.preliminaryRisk(), llmRisk);

        String explanation = llmVerdict.map(LlmVerdict::simpleExplanation)
                .filter(StringUtils::hasText)
                .orElseGet(() -> fallbackExplanation(finalRisk, hindi));
        List<String> safeActions = firstNonEmpty(
                llmVerdict.map(LlmVerdict::safeActions).orElse(List.of()),
                fallbackSafeActions(finalRisk, hindi));
        List<String> prohibitedActions = firstNonEmpty(
                llmVerdict.map(LlmVerdict::prohibitedActions).orElse(List.of()),
                fallbackProhibitedActions(hindi));
        String verificationSteps = llmVerdict.map(LlmVerdict::verificationSteps)
                .filter(StringUtils::hasText)
                .orElseGet(() -> fallbackVerificationSteps(hindi));

        String source = llmVerdict.isPresent() ? "RULE_ENGINE + LLM" : "RULE_ENGINE";

        return new AnalysisResponse(
                finalRisk,
                explanation,
                safeActions,
                prohibitedActions,
                verificationSteps,
                rule.signalLabels(),
                source,
                inputType,
                ocrRawText,
                Instant.now());
    }

    /** Max rule. The rule engine can only raise; the LLM can never lower a strong signal. */
    private static RiskLevel combineRisk(RiskLevel ruleRisk, RiskLevel llmRisk) {
        if (llmRisk == null) {
            return ruleRisk;
        }
        int maxOrdinal = Math.max(ruleRisk.ordinal(), llmRisk.ordinal());
        return RiskLevel.values()[maxOrdinal];
    }

    private static void requireText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Please paste the suspicious message or link to check.");
        }
    }

    private static List<String> firstNonEmpty(List<String> primary, List<String> fallback) {
        if (primary == null || primary.isEmpty()) {
            return fallback;
        }
        return primary;
    }

    // ------------------------------------------------------------------
    // Bilingual (English / Hinglish) fallback guidance used when the LLM
    // is unavailable. Conservative and always safe by default.
    // ------------------------------------------------------------------

    static String fallbackExplanation(RiskLevel risk, boolean hindi) {
        return switch (risk) {
            case HIGH -> hindi
                    ? "Ye message bahut khatarnak lagta hai. Ismein aapko jaldi mein action karne ya apna code/bataye jaane ke liye kaha ja raha hai. Aap is par bharosa mat karein."
                    : "This message looks very dangerous. It is trying to make you act fast or share a personal code. Do not trust it.";
            case MEDIUM -> hindi
                    ? "Is message mein kuch baatein sahi nahin lag rahin hain. Pehle kisi bharosemand vyakti se baat karein, aur official website ya app par khud check karein."
                    : "Some things in this message do not look right. First check with a trusted person, and confirm on the official website or app yourself.";
            case LOW -> hindi
                    ? "Ye message aam taur par bilkul thik lagta hai. Phir bhi, apna OTP, PIN ya password kisi ko kabhi na batayein."
                    : "This message looks normal and harmless. Still, never share your OTP, PIN or password with anyone.";
        };
    }

    static List<String> fallbackSafeActions(RiskLevel risk, boolean hindi) {
        return switch (risk) {
            case HIGH -> hindi
                    ? List.of("Ruk jayein — koi bhi link na dabayein aur koi code kisi ko na batayein",
                            "Turant apne bharosemand vyakti (parivaar ke kisi sadasy) ko batayein",
                            "Company ko unke official number par hi call karein")
                    : List.of("Stop — do not click any link and do not share any code",
                            "Tell your trusted family member or friend right away",
                            "Call the company only on their official number");
            case MEDIUM -> hindi
                    ? List.of("Kisi bharosemand vyakti se baat karne ke baad hi kuch karein",
                            "Official website ya app par khud jayein aur wahan check karein")
                    : List.of("Ask a trusted person before doing anything",
                            "Go to the official website or app yourself and check there");
            case LOW -> hindi
                    ? List.of("Aap chinta na karein — lekin OTP, PIN ya password kabhi share na karein",
                            "Agar man mein koi shak ho to kisi bharosemand vyakti se poochhein")
                    : List.of("You can relax — but never share OTP, PIN or passwords",
                            "If you feel unsure, double-check with a trusted person");
        };
    }

    static List<String> fallbackProhibitedActions(boolean hindi) {
        return hindi
                ? List.of("Message mein diya gaya koi bhi link/button na dabayein",
                        "OTP, UPI PIN, ATM PIN, password ya CVV kisi ko na batayein",
                        "Message mein diye gaye number par call na karein — sirf official number use karein")
                : List.of("Do not click any link or button in this message",
                        "Do not share OTP, UPI PIN, ATM PIN, password or CVV with anyone",
                        "Do not call a number given in the message — use only official contact details");
    }

    static String fallbackVerificationSteps(boolean hindi) {
        return hindi
                ? "Company ka official app ya website khud kholen (address khud type karein, message wala link use na karein). Agar koi aapse OTP ya PIN maange, to call karke cut karein aur official number par wapas call karein."
                : "Open the company's official app or website yourself (type the address; never use the link from the message). If someone asks for OTP or PIN, hang up and call the official number back.";
    }
}