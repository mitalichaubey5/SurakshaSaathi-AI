package com.surakshasaathi.ai.service.rule.impl;

import com.surakshasaathi.ai.service.rule.ScamRule;
import com.surakshasaathi.ai.service.rule.ScamSignal;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Flags shortened URLs (bit.ly, tinyurl.com, t.co, ow.ly, is.gd, rb.gy,
 * cutt.ly, goo.gl, shorturl.at, rebrand.ly, lnkd.in, tiny.cc, buff.ly,
 * adf.ly, v.gd, soo.gd ...). Shorteners hide the real destination and are a
 * favourite trick of scammers.
 */
public class ShortenedUrlRule implements ScamRule {

    public static final ScamSignal SIGNAL = new ScamSignal(
            "shortened_url",
            "Link is shortened and hides the real destination",
            2);

    static final Set<String> SHORTENERS = Set.of(
            "bit.ly", "tinyurl.com", "t.co", "ow.ly", "is.gd", "rb.gy",
            "cutt.ly", "goo.gl", "shorturl.at", "rebrand.ly", "lnkd.in",
            "tiny.cc", "buff.ly", "adf.ly", "v.gd", "soo.gd", "shorte.st");

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
            for (String shortener : SHORTENERS) {
                if (host.equals(shortener) || host.endsWith("." + shortener)) {
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