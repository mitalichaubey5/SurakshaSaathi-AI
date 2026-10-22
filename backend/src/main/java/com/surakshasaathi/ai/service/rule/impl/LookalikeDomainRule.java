package com.surakshasaathi.ai.service.rule.impl;

import com.surakshasaathi.ai.service.rule.ScamRule;
import com.surakshasaathi.ai.service.rule.ScamSignal;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Flags look-alike / typosquat domains that impersonate banks or apps,
 * e.g. "secure-bank-verify.com", "sbi-secure-login.com", "paytm-reward5432.com".
 *
 * Official well-known domains are allow-listed to avoid false positives.
 */
public class LookalikeDomainRule implements ScamRule {

    public static final ScamSignal SIGNAL = new ScamSignal(
            "lookalike_domain",
            "Domain is disguised to look like a trusted company",
            3);

    private static final Set<String> TRUST_WORDS = Set.of(
            "secure", "verify", "kyc", "confirm", "update", "alert",
            "login", "signin", "account", "aadhaar", "renew", "claim",
            "refund", "reward", "gift", "prize", "bonus", "debit", "credit");

    private static final Set<String> BRANDS = Set.of(
            "sbi", "hdfc", "icici", "axis", "kotak", "paytm", "phonepe",
            "gpay", "paypal", "whatsapp", "amazon", "flipkart", "irctc", "rbi");

    private static final Set<String> ALLOW_LIST = Set.of(
            "sbi.co.in", "hdfcbank.com", "icicibank.com", "axisbank.com",
            "kotak.com", "paytm.com", "phonepe.com", "yblpay.com",
            "npci.org.in", "rbi.org.in", "cybercrime.gov.in", "irctc.co.in",
            "google.com", "whatsapp.com", "facebook.com", "wikipedia.org");

    private static final Pattern URL = Pattern.compile("https?://[\\w\\-.]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern DIGIT = Pattern.compile("\\d");

    @Override
    public String id() {
        return SIGNAL.code();
    }

    @Override
    public Optional<ScamSignal> match(String text) {
        Matcher m = URL.matcher(text);
        while (m.find()) {
            String host = extractHost(m.group()).toLowerCase();
            if (host.isBlank() || ALLOW_LIST.contains(host)) {
                continue;
            }
            String label = host.substring(0, host.indexOf('.') >= 0 ? host.indexOf('.') : host.length());
            boolean containsTrustWord = TRUST_WORDS.stream().anyMatch(label::contains);
            boolean containsBrand = BRANDS.stream().anyMatch(label::contains);
            boolean obfuscated = label.contains("-") || DIGIT.matcher(label).find();
            if ((containsTrustWord || containsBrand) && obfuscated) {
                return Optional.of(SIGNAL);
            }
        }
        return Optional.empty();
    }

    private static String extractHost(String url) {
        String rest = url;
        int scheme = rest.indexOf("://");
        if (scheme >= 0) {
            rest = rest.substring(scheme + 3);
        }
        int slash = rest.indexOf('/');
        if (slash >= 0) {
            rest = rest.substring(0, slash);
        }
        return rest;
    }
}