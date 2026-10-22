package com.surakshasaathi.ai.service.rule;

import com.surakshasaathi.ai.service.rule.impl.LookalikeDomainRule;
import com.surakshasaathi.ai.service.rule.impl.ShortenedUrlRule;
import com.surakshasaathi.ai.service.rule.impl.SuspiciousTldRule;

import java.util.List;

/**
 * The rule catalog — the single place where detection rules are registered.
 *
 * To extend: add one {@link ScamRule} entry below (either a keyword pattern via
 * {@link ScamRule#of(String, ScamSignal, String)} or a custom rule class).
 * Severity: 1 = weak hint, 2 = moderate, 3 = strong scam signal.
 */
public final class ScamRules {

    private ScamRules() { }

    /** Strong signals — alone enough for HIGH risk. */
    public static final ScamSignal KYC_UPDATE = signal("kyc_update", "KYC update / expiry demanded", 3);
    public static final ScamSignal OTP_SHARE = signal("otp_share", "Asking you to reveal an OTP", 3);
    public static final ScamSignal UPI_PIN = signal("upi_pin", "Asking for UPI / ATM PIN or MPIN", 3);
    public static final ScamSignal ACCOUNT_BLOCKED = signal("account_blocked", "Threatening that an account is blocked", 3);
    public static final ScamSignal REWARD_PRIZE = signal("reward_prize", "Unexpected reward / prize / lottery", 3);
    public static final ScamSignal SHARE_BANK_DETAILS = signal("share_bank_details", "Asking you to share bank details or passwords", 3);

    /** Moderate signals — need verification. */
    public static final ScamSignal KYC = signal("kyc", "KYC mentioned", 2);
    public static final ScamSignal OTP = signal("otp", "OTP mentioned", 2);
    public static final ScamSignal URGENCY = signal("urgency", "Message pressures you to act fast", 2);
    public static final ScamSignal VERIFY_SUSPICION = signal("verify_suspicion", "Demands identity verification", 2);
    public static final ScamSignal PAYMENT_REQUEST = signal("payment_request", "Unexpected payment / refund message", 2);
    public static final ScamSignal BANK_URGENCY = signal("bank_urgency", "Bank / account related warning", 2);

    /** Returns the full, ordered rule list. */
    public static List<ScamRule> catalog() {
        return List.of(
                ScamRule.of(KYC_UPDATE.code(), KYC_UPDATE,
                        "\\bkyc\\b[^\\n]{0,80}\\b(update|expir|renew|verif)\\w*\\b"),
                ScamRule.of(KYC.code(), KYC, "\\bkyc\\b"),

                ScamRule.of(OTP_SHARE.code(), OTP_SHARE,
                        "\\botp\\b[^.!?\\n]{0,40}\\b(share|tell|send|reveal|confirm|verify|enter)\\b"
                                + "|\\b(share|tell|send|give|reveal|enter)\\b[^.!?\\n]{0,30}\\botp\\b"),
                ScamRule.of(OTP.code(), OTP, "\\botp\\b"),

                ScamRule.of(UPI_PIN.code(), UPI_PIN,
                        "\\bupi\\s*pin\\b|\\batm\\s*pin\\b|\\bmpin\\b"),

                ScamRule.of(ACCOUNT_BLOCKED.code(), ACCOUNT_BLOCKED,
                        "\\b(account|card|upi|wallet|banking?)\\b[^.!?\\n]{0,60}\\b(block|freeze|frozen|suspens|deactivat|terminat|closed)\\w*\\b"
                                + "|\\b(blocked|frozen|suspended|deactivated)\\b[^.!?\\n]{0,30}\\b(account|card|upi|wallet)\\b"),

                ScamRule.of(URGENCY.code(), URGENCY,
                        "\\b(urgent|urgently|immediately|expires?\\s+(today|soon|tonight)|right away|asap|act now|click now|last chance|within\\s+\\d+\\s*(hours|minutes|days))\\b"),

                ScamRule.of(VERIFY_SUSPICION.code(), VERIFY_SUSPICION,
                        "\\b(verify|confirm|validate|reactivat)\\w*\\b[^.!?\\n]{0,40}\\b(immediately|now|urgent)\\w*\\b"
                                + "|\\bimmediately\\b[^.!?\\n]{0,40}\\b(verify|confirm)\\w*\\b"),

                ScamRule.of(REWARD_PRIZE.code(), REWARD_PRIZE,
                        "\\b(you\\s+(have\\s+)?won|winner|win\\s+a|prize|jackpot|lottery|congratulations?|gift\\s+(voucher|card|reward)|cash\\s+(reward|prize)|free\\s+(gift|iphone|iphone|laptop|trip))\\b"),

                ScamRule.of(PAYMENT_REQUEST.code(), PAYMENT_REQUEST,
                        "\\b(payment|payout|refund|salary|cash)\\b[^.!?\\n]{0,40}\\b(received|credited|released|claim|approve|transfer)\\w*\\b"
                                + "|\\bupi\\s*collect\\s*request\\b|\\bcollect\\s*request\\b"),

                ScamRule.of(BANK_URGENCY.code(), BANK_URGENCY,
                        "\\b(bank|banking|account|card)\\b[^.!?\\n]{0,40}\\b(statement|login|limit|verif|suspicious|activity)\\w*\\b"),

                ScamRule.of(SHARE_BANK_DETAILS.code(), SHARE_BANK_DETAILS,
                        "\\bshare\\b[^.!?\\n]{0,30}\\b(bank\\s+details|banking\\s+details|card\\s+number|password|cvv)\\b"),

                // URL-based rules (custom logic)
                new ShortenedUrlRule(),
                new SuspiciousTldRule(),
                new LookalikeDomainRule()
        );
    }

    private static ScamSignal signal(String code, String label, int severity) {
        return new ScamSignal(code, label, severity);
    }
}