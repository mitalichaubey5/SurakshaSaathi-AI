package com.surakshasaathi.ai.service.rule.impl;

import com.surakshasaathi.ai.service.rule.ScamRule;
import com.surakshasaathi.ai.service.rule.ScamSignal;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Flags URLs hosted on free-to-register TLDs that are heavily abused by
 * scammers (SpamHaus top-scam TLDs): .xyz .top .gq .ml .cf .tk .buzz .icu
 * .click .loan .work .cyou .cam .rest .mom
 */
public class SuspiciousTldRule implements ScamRule {

    public static final ScamSignal SIGNAL = new ScamSignal(
            "suspicious_tld",
            "Link uses a high-risk free domain (.xyz, .top, etc.)",
            3);

    static final Set<String> ABUSED_TLDS = Set.of(
            "xyz", "top", "gq", "ml", "cf", "tk", "buzz", "icu",
            "click", "loan", "work", "cyou", "cam", "rest", "mom");

    private static final Pattern URL = Pattern.compile("https?://[\\w\\-.]+", Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return SIGNAL.code();
    }

    @Override
    public Optional<ScamSignal> match(String text) {
        Matcher m = URL.matcher(text);
        while (m.find()) {
            String host = extractHost(m.group());
            int dot = host.lastIndexOf('.');
            if (dot >= 0 && dot < host.length() - 1) {
                String tld = host.substring(dot + 1).toLowerCase();
                if (ABUSED_TLDS.contains(tld)) {
                    return Optional.of(SIGNAL);
                }
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