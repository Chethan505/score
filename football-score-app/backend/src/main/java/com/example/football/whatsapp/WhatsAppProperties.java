package com.example.football.whatsapp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "whatsapp")
public class WhatsAppProperties {

    private boolean enabled = true;
    private String accessToken;
    private String phoneNumberId;
    private String wabaId;
    private String webhookVerifyToken;
    private String appSecret;
    private String baseUrl = "https://graph.facebook.com/v20.0";
    private int maxRetries = 3;
    private int retryBaseDelayMs = 5000;
    private int retryMaxDelayMs = 60000;
    private int rateLimitPerSecond = 80;
    private int pairRateLimitPerMinute = 10;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getPhoneNumberId() { return phoneNumberId; }
    public void setPhoneNumberId(String phoneNumberId) { this.phoneNumberId = phoneNumberId; }
    public String getWabaId() { return wabaId; }
    public void setWabaId(String wabaId) { this.wabaId = wabaId; }
    public String getWebhookVerifyToken() { return webhookVerifyToken; }
    public void setWebhookVerifyToken(String webhookVerifyToken) { this.webhookVerifyToken = webhookVerifyToken; }
    public String getAppSecret() { return appSecret; }
    public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public int getRetryBaseDelayMs() { return retryBaseDelayMs; }
    public void setRetryBaseDelayMs(int retryBaseDelayMs) { this.retryBaseDelayMs = retryBaseDelayMs; }
    public int getRetryMaxDelayMs() { return retryMaxDelayMs; }
    public void setRetryMaxDelayMs(int retryMaxDelayMs) { this.retryMaxDelayMs = retryMaxDelayMs; }
    public int getRateLimitPerSecond() { return rateLimitPerSecond; }
    public void setRateLimitPerSecond(int rateLimitPerSecond) { this.rateLimitPerSecond = rateLimitPerSecond; }
    public int getPairRateLimitPerMinute() { return pairRateLimitPerMinute; }
    public void setPairRateLimitPerMinute(int pairRateLimitPerMinute) { this.pairRateLimitPerMinute = pairRateLimitPerMinute; }
}