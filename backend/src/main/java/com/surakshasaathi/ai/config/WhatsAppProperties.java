package com.surakshasaathi.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * WhatsApp Cloud API configuration.
 *
 * All secrets come from environment variables (see application.yml) and are
 * never hardcoded. WhatsApp sending stays disabled by default ({@code enabled=false})
 * until explicitly turned on; Phase 1 only uses the webhook verification values.
 */
@Configuration
@ConfigurationProperties(prefix = "app.whatsapp")
public class WhatsAppProperties {

    /** Master switch. Kept false by default so nothing is ever sent out. */
    private boolean enabled = false;

    /**
     * Second, independent switch that gates <em>sending</em> only.
     * Env: WHATSAPP_SENDING_ENABLED. A real send requires both {@code enabled=true}
     * and {@code sendingEnabled=true}, plus valid credentials and an allow-listed
     * recipient. The webhook stays available regardless of this flag.
     */
    private boolean sendingEnabled = false;


    /** Meta Cloud API access token (never logged). Env: WHATSAPP_ACCESS_TOKEN. */
    private String accessToken = "";

    /** WhatsApp Business phone number id used as the message sender. Env: WHATSAPP_PHONE_NUMBER_ID. */
    private String phoneNumberId = "";

    /** Meta app secret used to validate X-Hub-Signature-256 on webhook POSTs. Env: WHATSAPP_APP_SECRET. */
    private String appSecret = "";

    /** Token echoed back during the GET webhook verification challenge. Env: WHATSAPP_WEBHOOK_VERIFY_TOKEN. */
    private String verifyToken = "";

    /** The single explicitly permitted test recipient in E.164 form. Env: WHATSAPP_TEST_RECIPIENT. */
    private String testRecipient = "";

    /** Optional additional permitted test recipients (comma-separated). Env: WHATSAPP_TEST_RECIPIENTS. */
    private String testRecipients = "";

    /** Graph API base URL. */
    private String graphBaseUrl = "https://graph.facebook.com";

    /** Graph API version to pin. */
    private String apiVersion = "v21.0";

    /** Pre-approved template name (used only in a later sending phase). */
    private String templateName = "hello_world";

    /** Template language code (must match the approved template). */
    private String templateLanguage = "en_US";

    /** Outbound HTTP timeout (seconds) for a later sending phase. */
    private int timeoutSeconds = 15;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSendingEnabled() {
        return sendingEnabled;
    }

    public void setSendingEnabled(boolean sendingEnabled) {
        this.sendingEnabled = sendingEnabled;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getPhoneNumberId() {
        return phoneNumberId;
    }

    public void setPhoneNumberId(String phoneNumberId) {
        this.phoneNumberId = phoneNumberId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getVerifyToken() {
        return verifyToken;
    }

    public void setVerifyToken(String verifyToken) {
        this.verifyToken = verifyToken;
    }

    public String getTestRecipient() {
        return testRecipient;
    }

    public void setTestRecipient(String testRecipient) {
        this.testRecipient = testRecipient;
    }

    public String getTestRecipients() {
        return testRecipients;
    }

    public void setTestRecipients(String testRecipients) {
        this.testRecipients = testRecipients;
    }

    public String getGraphBaseUrl() {
        return graphBaseUrl;
    }

    public void setGraphBaseUrl(String graphBaseUrl) {
        this.graphBaseUrl = graphBaseUrl;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateLanguage() {
        return templateLanguage;
    }

    public void setTemplateLanguage(String templateLanguage) {
        this.templateLanguage = templateLanguage;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
